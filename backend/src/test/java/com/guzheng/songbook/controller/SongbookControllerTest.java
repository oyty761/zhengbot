package com.guzheng.songbook.controller;

import com.guzheng.songbook.dto.SongbookDtos;
import com.guzheng.songbook.service.SongbookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SongbookController.class)
class SongbookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SongbookService songbookService;

    @Test
    void homeUsesUnifiedApiResponse() throws Exception {
        when(songbookService.getHome()).thenReturn(new SongbookDtos.HomeResponse(List.of(
                new SongbookDtos.ModuleItem("曲库点歌", "LIBRARY", "浏览曲库")
        )));

        mockMvc.perform(get("/api/songbook/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.modules[0].type").value("LIBRARY"));
    }

    @Test
    void blankSearchContentReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/songbook/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": " ",
                                  "inputChannel": "TEXT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("点歌需求不能为空"));
    }
}
