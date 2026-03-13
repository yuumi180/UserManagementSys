package com.example.demo.utils;

import com.example.demo.entity.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtils {

    /**
     * 导出用户数据到 Excel
     */
    public static void exportUsers(List<User> userList, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户列表");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "用户名", "昵称", "年龄", "性别", "地址"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        // 创建数据行
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            Row row = sheet.createRow(i + 1);
            
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getUsername());
            row.createCell(2).setCellValue(user.getNickname());
            row.createCell(3).setCellValue(user.getAge() != null ? user.getAge() : 0);
            row.createCell(4).setCellValue(user.getSex() != null ? user.getSex() : "");
            row.createCell(5).setCellValue(user.getAddress() != null ? user.getAddress() : "");
        }

        // 设置列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=用户列表.xlsx");

        // 输出到浏览器
        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 从 Excel 导入用户数据
     */
    public static List<User> importUsers(InputStream inputStream) throws IOException {
        List<User> userList = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        // 从第一行开始（跳过标题行）
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            User user = new User();
            
            Cell usernameCell = row.getCell(1);
            if (usernameCell != null) {
                user.setUsername(usernameCell.getStringCellValue());
            }
            
            Cell nicknameCell = row.getCell(2);
            if (nicknameCell != null) {
                user.setNickname(nicknameCell.getStringCellValue());
            }
            
            Cell ageCell = row.getCell(3);
            if (ageCell != null) {
                try {
                    user.setAge((int) ageCell.getNumericCellValue());
                } catch (Exception e) {
                    user.setAge(null);
                }
            }
            
            Cell sexCell = row.getCell(4);
            if (sexCell != null) {
                user.setSex(sexCell.getStringCellValue());
            }
            
            Cell addressCell = row.getCell(5);
            if (addressCell != null) {
                user.setAddress(addressCell.getStringCellValue());
            }

            // 设置默认密码
            user.setPassword(PasswordEncoder.encode("123456"));

            userList.add(user);
        }

        workbook.close();
        inputStream.close();
        
        return userList;
    }
}
