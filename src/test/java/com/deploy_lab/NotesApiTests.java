package com.deploy_lab;

import com.deploy_lab.note.Note;
import com.deploy_lab.note.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotesApiTests {
    @Autowired MockMvc mvc;
    @Autowired NoteRepository repository;

    @BeforeEach
    void clearNotes() {
        repository.deleteAll();
    }

    @Test
    void createsPersistsAndRetrievesNote() throws Exception {
        String location = mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  Học DevOps  ","content":"  Chạy Spring Boot với MySQL  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Học DevOps"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getHeader("Location");
        assertThat(repository.count()).isEqualTo(1);
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Học DevOps"))
                .andExpect(jsonPath("$.content").value("Chạy Spring Boot với MySQL"));
    }

    @Test
    void listsNewestNotesFirst() throws Exception {
        repository.save(new Note("First", "First content"));
        repository.save(new Note("Second", "Second content"));
        mvc.perform(get("/api/notes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Second"));
    }

    @Test
    void returnsEmptyList() throws Exception {
        mvc.perform(get("/api/notes")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void missingNoteReturns404() throws Exception {
        mvc.perform(get("/api/notes/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void invalidIdReturns400(String id) throws Exception {
        mvc.perform(get("/api/notes/" + id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":\" \",\"content\":\" \"}", "{\"title\":null,\"content\":null}"})
    void requiredFieldsReturn400(String body) throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.content").exists());
        assertThat(repository.count()).isZero();
    }

    @Test
    void oversizedFieldsReturn400() throws Exception {
        String body = "{\"title\":\"" + "a".repeat(201) + "\",\"content\":\"" + "b".repeat(5001) + "\"}";
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.content").exists());
        assertThat(repository.count()).isZero();
    }

    @Test
    void acceptsMaximumLengths() throws Exception {
        String body = "{\"title\":\"" + "a".repeat(200) + "\",\"content\":\"" + "b".repeat(5000) + "\"}";
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", ""})
    void malformedOrMissingBodyReturns400(String body) throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void healthIsPublicAndOtherActuatorsAreNotExposed() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }
}
