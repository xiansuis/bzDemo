package com.example.demo.util.fbUtil;

import com.example.demo.checkFb.FileSearch;
import com.example.demo.jpa.pMass.entity.PmPatchReg;
import com.example.demo.util.commonUtil.Application;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DownloadFile {

    /**
     * 实现附件下载以及备份的主方法
     */
    public void downLoanPMassFile(List<PmPatchReg> entityList, String environment, String excelSuffix, String url,String backDir) throws Exception {
        if ((entityList != null && entityList.size() == 0)) {
            throw new Exception("查询到的数据为空，请检查是否所需下载的补丁编号是否为空");
        } else {
            //校验参数合法性
            checkEntity(Objects.requireNonNull(entityList));
            //备份文件
            backFileDir(url, backDir);
            //删除文件
            deleteFolderContents(new File(url));
            //下载文件
            downloadFileToDo(url, entityList,environment,excelSuffix);
        }
    }

    /**
     * 校验参数合法性
     */
    public static void checkEntity(List<PmPatchReg> list) throws Exception {
        for (PmPatchReg pmPatchReg : list) {
            objectChangeString(pmPatchReg.getPatchCode(), true, "补丁编号");
            objectChangeString(pmPatchReg.getPatchDever(), true, "补丁登记人");
            objectChangeString(pmPatchReg.getPatchDisc(), true, "补丁描述");
            objectChangeString(Arrays.toString(pmPatchReg.getFileBody()), true, "补丁附件");
            objectChangeString(pmPatchReg.getFileName(), true, "附件名称");
        }
    }

    /**
     * 校验参数
     */
    public static void objectChangeString(String data, Boolean flag, String name) throws Exception {
        if (flag && StringUtils.isEmpty(data)) {
            throw new Exception(name + "不能为空");
        }
    }

    /**
     * 判断当前路径  是文件夹还是文件，然后生成
     */
    public static boolean makeFile(String filePath, String flag) throws Exception {
        if (StringUtils.isEmpty(filePath)) {
            throw new Exception("文件路径不能为空");
        }
        boolean fileFlag = true;
        try {
            File newFile = new File(filePath);
            if (!newFile.exists() && "path".endsWith(flag)) {
                fileFlag = newFile.mkdirs();
            } else if (!newFile.exists() && "file".endsWith(flag)) {
                fileFlag = newFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return fileFlag;
    }


    /**
     * 文件删除
     */
    private static void deleteFolderContents(File folder) {
        File[] files = folder.listFiles();
        boolean flag;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是子文件夹，则递归地删除子文件夹中的内容
                    deleteFolderContents(file);
                } else {
                    // 如果是文件，则删除文件
                    flag = file.delete();
                }
            }
        }
        // 删除空文件夹
        flag = folder.delete();
    }

    /**
     * 获取文件拼接的路径
     */
    public static String getPath(String patchCode, String patchDisc, String fileName, String pMassDir) throws Exception {
        //获取根目录
        String path;
        if ((patchDisc.contains("【柜面】") || patchDisc.contains("[柜面]")) && fileName.endsWith("tar")) {
            path = "gm";
        } else if ((patchDisc.contains("【网银】") || patchDisc.contains("[网银]")) && fileName.endsWith("tar")) {
            path = "wy";
        } else if (patchDisc.contains("【报表】") || patchDisc.contains("[报表]")) {
            path = "报表";
        } else if (patchDisc.contains("【调度】") || patchDisc.contains("[调度]")) {
            path = "调度";
        } else if ((patchDisc.contains("【柜面】") || patchDisc.contains("[柜面]")) && fileName.endsWith("txt")) {
            path = "gmSql";
        } else if (patchDisc.contains("【工作流】") || patchDisc.contains("[工作流]")) {
            path = "工作流";
        } else if ((patchDisc.contains("【网银】") || patchDisc.contains("[网银]")) && fileName.endsWith("txt")) {
            path = "wySql";
        } else {
            throw new Exception("补丁描述错误:" + patchCode);
        }
        return pMassDir + "\\" + path;
    }

    /**
     * 从文件中获取补丁编号
     */
    public String loadFileAsString() throws IOException {
        ClassPathResource resource = new ClassPathResource("pmass.txt");
        byte[] fileData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(fileData, StandardCharsets.UTF_8);
    }

    /**
     * 移动文件夹
     */
    private static void moveFolderContents(File sourceFolder, File destinationFolder) throws IOException {
        // 获取源文件夹中的所有文件和子文件夹
        File[] files = sourceFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                File newFile = new File(destinationFolder.getAbsolutePath() + File.separator + file.getName());
                if (!newFile.exists()) {
                    boolean flag = newFile.mkdirs();
                }
                if (file.isDirectory()) {
                    // 如果是子文件夹，则递归地移动子文件夹中的内容
                    moveFolderContents(file, newFile);
                } else {
                    // 如果是文件，则移动文件到目标文件夹
                    Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * 备份文件
     */
    public static void backFileDir(String downloadUrl,String backDir) throws Exception {
        //获取上次的下载目录
        String lastDownloadUrl=getLastDownLoadPath(downloadUrl);
        if(StringUtils.isNotEmpty(lastDownloadUrl)){
            //判断目录有无数据 没有则创建
            File fileDir = new File(lastDownloadUrl);
            File[] files = fileDir.listFiles();
            if (files != null && files.length > 0) {
                moveFolderContents(new File(lastDownloadUrl+ "\\"), new File(backDir));
                deleteFolderContents(fileDir);
            }
        }
    }



    /**
     * 下载对应的附件
     */
    public static void downloadFileToDo(String url, List<PmPatchReg> entityList,String environment,String excelSuffix) throws Exception {
        for (PmPatchReg pmPatchReg : entityList) {
            String fileName = pmPatchReg.getFileName();//文件名
            String patchDisc = pmPatchReg.getPatchDisc();//补丁描述
            String patchCode = pmPatchReg.getPatchCode();//补丁编号
            //根据补丁描述获取补丁存放路径
            String filePath = getPath(patchCode, patchDisc, fileName, url);
            String absoluteName = patchCode + "_" + fileName;
            //生成文件
            try {
                boolean fileFlag = makeFile(filePath, "path");
                fileFlag = makeFile(filePath + "\\" + absoluteName, "file");
                File attachFile = new File(filePath + "\\" + absoluteName);
                FileOutputStream fileOutputStream = new FileOutputStream(attachFile);
                fileOutputStream.write(pmPatchReg.getFileBody());
                fileOutputStream.close();
                log.info(absoluteName + "   文件已成功生成!");
            } catch (Exception e) {
                log.info(absoluteName + "   生成文件时出现错误：" + e.getMessage());
                throw new Exception(e.getMessage());
            }
        }
        //移动excel到内部文件夹，防止下次操作失误，导致发版出错
        if ("product".equals(environment)) {
            String excelUrl = FileSearch.getExcelPath(url, excelSuffix);
            String removeUrl = url + "\\" + FileSearch.getExcelName(url, excelSuffix);
            File file = new File(excelUrl);
            File newFile = new File(removeUrl);
            if (!newFile.exists()) {
                boolean flag = newFile.mkdirs();
            }
            Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 获取上次下载目录文件夹
     */
    public static String getLastDownLoadPath(String nowPath){
        String lastDownloadPath=FileSearch.getPrefix(nowPath);
        String returnPath=nowPath;
        File file=new File(lastDownloadPath);
        String nowDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            String name= files[i].getName();
            String path=files[i].getPath();
            if(new File(path).isDirectory()&& !name.equals(nowDate)&&name.length()==8&&StringUtils.isNumeric(name)){
                returnPath=path;
                break;
            }
        }
        return returnPath;
    }

}
