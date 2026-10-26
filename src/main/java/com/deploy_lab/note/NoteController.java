package com.deploy_lab.note;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest request) {
        NoteResponse note = service.create(request);
        return ResponseEntity.created(URI.create("/api/notes/" + note.id())).body(note);
    }

    @GetMapping
    public List<NoteResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public NoteResponse findById(@PathVariable @Positive(message = "Id must be positive") Long id) {
        return service.findById(id);
    }
}
