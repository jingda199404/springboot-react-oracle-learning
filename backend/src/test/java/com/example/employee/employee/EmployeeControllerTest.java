package com.example.employee.employee;

import com.example.employee.auth.AppUser;
import com.example.employee.auth.AppUserRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void createsAndListsEmployee() throws Exception {
        Long userId = employeeUserId("employee_test_user_1");
        String request = """
                {
                  "name": "山田太郎",
                  "email": "yamada@example.com",
                  "department": "開発部",
                  "salary": 18000,
                  "hireDate": "2026-06-10"
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("山田太郎"));

        mockMvc.perform(get("/api/employees").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("yamada@example.com"));
    }

    @Test
    void downloadsTemplateExportsAndImportsExcel() throws Exception {
        Long userId = employeeUserId("employee_test_user_2");
        byte[] template = mockMvc.perform(get("/api/employees/template").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "employee-upload-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                template
        );

        mockMvc.perform(multipart("/api/employees/import").file(upload).header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));

        mockMvc.perform(get("/api/employees/export").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private Long employeeUserId(String username) {
        return userRepository.save(new AppUser(username, "test-password-hash", "ACCOUNTING,EMPLOYEE")).getId();
    }
}
