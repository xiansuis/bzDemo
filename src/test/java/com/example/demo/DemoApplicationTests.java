package com.example.demo;

import com.example.demo.checkFb.FileSearch;
import com.example.demo.checkFb.GetPMassId;
import com.example.demo.jpa.pMass.dao.PMassDao;
import com.example.demo.jpa.pMass.entity.PmPatchReg;
import com.example.demo.util.commonUtil.Application;
import com.example.demo.util.commonUtil.ConvertMapToObject;
import com.example.demo.util.fbUtil.DownloadFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
class DemoApplicationTests {
    @Value("${pmass_download_dir}")
    private String url;
    @Value("${environment}")
    private String environment;
    @Value("${excelSuffix}")
    private String excelSuffix;
    @Value("${pmass_log_dir}")
    private String logPath;
    @Value("${pmass_backFile_dir}")
    private String historyPath;
    @Autowired
    private PMassDao pMassDao;

    private DownloadFile downloadFile=new DownloadFile();
    @Test
    @Rollback(false)
    public void downLoadFile() throws Exception {
        try {
            String nowDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String backDir =historyPath + "\\" + new SimpleDateFormat("yyyyMMdd").format(new Date())
                    + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            this.url=url+"\\"+nowDate;
            //判断文件是否存在并生成对应的文件夹
            FileSearch.checkFileExists(logPath,url,historyPath);
            //获取补丁编号
            List<String> patchCodeList=getPatchCodeList();
            //查询数据
            List<PmPatchReg> entityList = ConvertMapToObject.convertMapToObject(pMassDao.queryAllByPatchCode(patchCodeList), PmPatchReg.class,true);
            //校验查询到的数据是否正确
            FileSearch.checkPMassPatch(pMassDao.queryList(patchCodeList), patchCodeList);
            //校验是否能够发版
//            assert entityList != null;
//            FbUtil.checkState(entityList,environment);
            //生成附件
            downloadFile.downLoanPMassFile(entityList, environment, excelSuffix,url,backDir);
            if ("product".equals(environment)) {
                //检查依赖
                FileSearch.startCheck(url);
                //发版准备结束以后把根目录下的excel删除，防止下次发版操作出错
                File excelFile = new File(FileSearch.getExcelPath(url, excelSuffix));
                boolean flag = excelFile.delete();
            }
            //默认暂时不更新，若要启用，请谨慎使用
            //updateStat(environment,patchCodeList);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    /**
     * 更新状态
     * sit-已发测试环境
     * uat-验证完毕
     * product-已投产
     */
    public void updateStat(String flag, List<String> patchCode) throws Exception {
        String stat = FileSearch.getMap(flag);
        //此处最好直接替换成自己pmass的id
        String username = System.getProperty("user.name");
        String date = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date());
        int num = pMassDao.updateStat(stat, username, date, patchCode);
        log.info("更新的数量一共为：" + num);
    }

    /**
     * 获取补丁编号并去重
     */
    public List<String> getPatchCodeList() throws Exception {
        List<String> patchCodeList = new ArrayList<>();
        //获取需要下载的补丁编号
        //测试环境需要将补丁编号放置在pmass上，生产环境则为excel放置在配置的pmass_download_dir
        if ("uat".equals(environment) || "sit".equals(environment)) {
            patchCodeList = Arrays.asList(downloadFile.loadFileAsString().split(";"));
        } else if ("product".equals(environment)) {
            patchCodeList = GetPMassId.getPMassId(FileSearch.getExcelPath(url, excelSuffix));
        }
        //trim()
        patchCodeList = patchCodeList.stream().map(String::trim).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
        log.info("获取到的补丁编号为：" + String.join(";", patchCodeList));
        return patchCodeList;
    }
}
