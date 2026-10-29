package edu.symposium.web;

import edu.symposium.domain.Paper;
import edu.symposium.domain.PaperStatus;
import edu.symposium.repository.ConferenceRepository;
import edu.symposium.repository.PaperRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PaperController {
    private final PaperRepository papers;
    private final ConferenceRepository conferences;

    public PaperController(PaperRepository papers, ConferenceRepository conferences) {
        this.papers = papers;
        this.conferences = conferences;
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','REVIEWER')")
    public List<Paper> list() {
        return papers.findAll();
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public Paper submit(@RequestParam @NonNull Long conferenceId, @RequestBody Paper paper) {
        paper.setConference(conferences.findById(conferenceId).orElseThrow());
        return papers.save(paper);
    }

    @PatchMapping("/submissions/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','REVIEWER')")
    public Paper status(@PathVariable @NonNull Long id, @RequestParam PaperStatus value) {
        Paper paper = papers.findById(id).orElseThrow();
        paper.setStatus(value);
        return papers.save(paper);
    }
}

