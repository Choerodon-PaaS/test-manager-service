package io.choerodon.test.manager.app.service.impl

import io.choerodon.agile.api.dto.IssueDTO
import io.choerodon.core.convertor.ConvertHelper
import io.choerodon.core.oauth.CustomUserDetails
import io.choerodon.core.oauth.DetailsHelper
import io.choerodon.test.manager.IntegrationTestConfiguration
import io.choerodon.test.manager.app.service.ExcelImportService
import io.choerodon.test.manager.app.service.FileService
import io.choerodon.test.manager.app.service.NotifyService
import io.choerodon.test.manager.app.service.TestCaseService
import io.choerodon.test.manager.app.service.TestFileLoadHistoryService
import io.choerodon.test.manager.domain.service.ITestFileLoadHistoryService
import io.choerodon.test.manager.domain.service.impl.IExcelImportServiceImpl
import io.choerodon.test.manager.domain.test.manager.entity.TestFileLoadHistoryE
import io.choerodon.test.manager.infra.common.utils.ExcelUtil
import io.choerodon.test.manager.infra.common.utils.SpringUtil
import io.choerodon.test.manager.infra.dataobject.TestFileLoadHistoryDO
import io.choerodon.test.manager.infra.mapper.TestFileLoadHistoryMapper
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.util.AopTestUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import javax.servlet.http.HttpServletResponse

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
//@Stepwise
class ExcelImportServiceImplSpec extends Specification {

    @Autowired
    private ExcelImportService excelImportService

    @Autowired
    private TestFileLoadHistoryService testFileLoadHistoryService

    @Autowired
    private TestFileLoadHistoryMapper loadHistoryMapper

    @Autowired
    private ITestFileLoadHistoryService iTestFileLoadHistoryService

    @Shared
    private CustomUserDetails userDetails

    @Autowired
    NotifyService notifyService

    @Autowired
    TestCaseService testCaseService;

    @Autowired
    FileService fileService

    def setupSpec() {
        userDetails = new CustomUserDetails("test", "12345678", Collections.emptyList())
        userDetails.setUserId(0L)
    }

    def "downloadImportTemp"() {
        given:
        HttpServletResponse response = new MockHttpServletResponse()
        when:
        excelImportService.downloadImportTemp(response)
        then:
        with(response) {
            status == HttpStatus.OK.value()
            contentType == "application/vnd.ms-excel"
            characterEncoding == "UTF-8"
        }
    }

    def "queryLatestImportIssueHistory"() {
        given:
        TestFileLoadHistoryE testFileLoadHistoryE = SpringUtil.getApplicationContext().getBean(TestFileLoadHistoryE)
        testFileLoadHistoryE.setCreatedBy(userDetails.userId)
        when:
        testFileLoadHistoryE = iTestFileLoadHistoryService.queryLatestImportIssueHistory(testFileLoadHistoryE)
        then:
        with(testFileLoadHistoryE) {
            id == 40
            projectId == 0
            actionType == 1
            sourceType == 0
            status == 1
            createdBy == 0
        }
    }

    def "importIssueByExcel1"(){
        given:
        HSSFWorkbook workbook=new HSSFWorkbook()
        workbook.createSheet("测试用例")
        ExcelImportService service=AopTestUtils.getTargetObject(excelImportService);
        when:
        service.importIssueByExcel(144,4L,1L,workbook)
        then:
        1*notifyService.postWebSocket(_,_,_)
    }


    def "importIssueByExcel2"(){
        given:
        HSSFWorkbook workbook=new HSSFWorkbook()
        Sheet sheet=workbook.createSheet("测试用例")
        ExcelUtil.createRow(sheet,0,null)
        Row row=ExcelUtil.createRow(sheet,1,null)
        Row row2=ExcelUtil.createRow(sheet,2,null)
        ExcelUtil.createCell(row,0, ExcelUtil.CellType.TEXT,"概要2")
        ExcelUtil.createCell(row2,2, ExcelUtil.CellType.TEXT,"step")
        ExcelImportService service=AopTestUtils.getTargetObject(excelImportService);
        when:
        service.importIssueByExcel(144,4L,1L,workbook)
        then:
        3*notifyService.postWebSocket(_,_,_)
        1*testCaseService.createTest(_,_,_)>>new IssueDTO(issueId: 199L)
    }

    def "importIssueByExcel3"(){
        given:
        HSSFWorkbook workbook=new HSSFWorkbook()
        Sheet sheet=workbook.createSheet("测试用例")
        ExcelUtil.createRow(sheet,0,null)
        Row row=ExcelUtil.createRow(sheet,1,null)
        Row row2=ExcelUtil.createRow(sheet,2,null)
        ExcelUtil.createCell(row,0, ExcelUtil.CellType.TEXT,"概要2")
        ExcelUtil.createCell(row2,2, ExcelUtil.CellType.TEXT,"step")
        ExcelImportService service=AopTestUtils.getTargetObject(excelImportService);
        when:
        service.importIssueByExcel(144,4L,1L,workbook)
        then:
        3*notifyService.postWebSocket(_,_,_)
        1*testCaseService.createTest(_,_,_)>>null
        1*fileService.uploadFile(_,_,_)>>new ResponseEntity("url",HttpStatus.OK)
    }

    def "importIssueByExcel4"(){
        given:
        HSSFWorkbook workbook=new HSSFWorkbook()
        Sheet sheet=workbook.createSheet("测试用例")
        ExcelUtil.createRow(sheet,0,null)
        Row row=ExcelUtil.createRow(sheet,1,null)
        Row row2=ExcelUtil.createRow(sheet,2,null)
        ExcelUtil.createCell(row,0, ExcelUtil.CellType.TEXT,"概要2")
        ExcelUtil.createCell(row2,2, ExcelUtil.CellType.TEXT,"step")
        ExcelImportService service=AopTestUtils.getTargetObject(excelImportService);
        when:
        service.importIssueByExcel(144,4L,1L,workbook)
        then:
        3*notifyService.postWebSocket(_,_,_)
        1*testCaseService.createTest(_,_,_)>>null
        1*fileService.uploadFile(_,_,_)>>new ResponseEntity("url",HttpStatus.GATEWAY_TIMEOUT)
    }

    def "importExcel5"(){
        given:
        IExcelImportServiceImpl iExcelImportService=new IExcelImportServiceImpl();
        HSSFWorkbook workbook=new HSSFWorkbook()
        Row row=ExcelUtil.createRow(workbook.createSheet(),0,null)
        when:
        def re=iExcelImportService.processIssueHeaderRow(row,144L,2L,1L)
        then:
        re==null
    }

}