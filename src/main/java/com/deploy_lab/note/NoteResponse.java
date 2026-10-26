package com.deploy_lab.note;

import java.time.Instant;

public record NoteResponse(Long id, String title, String content, Instant createdAt) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getCreatedAt());
    }
}
