package edu.symposium.web;

import edu.symposium.domain.Review;
import edu.symposium.domain.PaperStatus;
import edu.symposium.repository.PaperRepository;
import edu.symposium.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewRepository reviews; private final PaperRepository papers;
    public ReviewController(ReviewRepository reviews, PaperRepository papers) { this.reviews = reviews; this.papers = papers; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','REVIEWER')")
    public Review submit(@RequestParam @NonNull Long paperId, @RequestBody Review review) {
        var paper = papers.findById(paperId).orElseThrow(); review.setPaper(paper);
        paper.setStatus(review.getRecommendation() != null && review.getRecommendation().contains("Reject") ? PaperStatus.REJECTED : PaperStatus.ACCEPTED);
        papers.save(paper); return reviews.save(review);
    }
}
