package com.deploy_lab.note;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoteService {
    private final NoteRepository repository;

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NoteResponse create(CreateNoteRequest request) {
        return NoteResponse.from(repository.save(new Note(request.title().strip(), request.content().strip())));
    }

    public List<NoteResponse> findAll() {
        return repository.findAllByOrderByIdDesc().stream().map(NoteResponse::from).toList();
    }

    public NoteResponse findById(Long id) {
        return repository.findById(id).map(NoteResponse::from)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }
}
