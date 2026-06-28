package com.example.employee.auth;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersAndLogsIn() throws Exception {
        String request = """
                {
                  "username": "learning_user",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("learning_user"))
                .andExpect(jsonPath("$.permissions[0]").value("ACCOUNTING"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(hasItem("ACCOUNTING")))
                .andExpect(jsonPath("$.message").value("ログインしました"))
                .andExpect(jsonPath("$.sessionToken").value(notNullValue()))
                .andReturn();

        String sessionToken = JsonPath.read(login.getResponse().getContentAsString(), "$.sessionToken");
        MvcResult secondLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").value(notNullValue()))
                .andExpect(jsonPath("$.sessionToken").value(not(sessionToken)))
                .andReturn();

        Integer userId = JsonPath.read(login.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get("/api/auth/session")
                        .header("X-User-Id", userId)
                        .header("X-Session-Token", sessionToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("他の端末でログインされたため、再ログインしてください"));

        String secondSessionToken = JsonPath.read(secondLogin.getResponse().getContentAsString(), "$.sessionToken");
        mockMvc.perform(post("/api/auth/logout")
                        .header("X-User-Id", userId)
                        .header("X-Session-Token", secondSessionToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").value(notNullValue()));
    }

    @Test
    void managesUsersWithPasswordAndExcelImport() throws Exception {
        String adminRequest = """
                {
                  "username": "admin",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminRequest))
                .andExpect(status().isCreated());

        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminRequest))
                .andExpect(status().isOk())
                .andReturn();
        Integer adminId = JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.id");
        String adminToken = JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.sessionToken");

        byte[] template = mockMvc.perform(get("/api/users/template")
                        .header("X-User-Id", adminId)
                        .header("X-Session-Token", adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        byte[] uploadBytes;
        try (Workbook workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(template))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.createRow(3);
            row.createCell(0).setCellValue("excel_user");
            row.createCell(1).setCellValue("password123");
            row.createCell(2).setCellValue("ACCOUNTING");
            try (java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                workbook.write(output);
                uploadBytes = output.toByteArray();
            }
        }

        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "user-upload-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                uploadBytes
        );
        mockMvc.perform(multipart("/api/users/import")
                        .file(upload)
                        .header("X-User-Id", adminId)
                        .header("X-Session-Token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(3));

        MvcResult users = mockMvc.perform(get("/api/users")
                        .header("X-User-Id", adminId)
                        .header("X-Session-Token", adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Integer> excelUserIds = JsonPath.read(users.getResponse().getContentAsString(), "$[?(@.username=='excel_user')].id");
        Integer excelUserId = excelUserIds.get(0);

        mockMvc.perform(put("/api/users/" + excelUserId + "/password")
                        .header("X-User-Id", adminId)
                        .header("X-Session-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"newpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("excel_user"));
    }
}
