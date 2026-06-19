package it.northleap.backend.controllers;

import it.northleap.backend.dtos.CreatePageDto;
import it.northleap.backend.dtos.UpdatePageDto;
import it.northleap.backend.entities.Page;
import it.northleap.backend.security.PermAction;
import it.northleap.backend.security.RequirePerm;
import it.northleap.backend.services.PageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Porting di PagesController (pages.module.ts).
@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;

    @GetMapping
    @RequirePerm(resource = "page", action = PermAction.READ)
    public ResponseEntity<List<Page>> list() {
        return ResponseEntity.ok(pageService.list());
    }

    @GetMapping("/{key}")
    @RequirePerm(resource = "page", action = PermAction.READ)
    public ResponseEntity<Page> get(@PathVariable String key) {
        return ResponseEntity.ok(pageService.get(key));
    }

    @PostMapping
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<Page> create(@Valid @RequestBody CreatePageDto dto) {
        return ResponseEntity.ok(pageService.create(dto));
    }

    @PatchMapping("/{key}")
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<Page> update(@PathVariable String key, @RequestBody UpdatePageDto dto) {
        return ResponseEntity.ok(pageService.update(key, dto));
    }

    @DeleteMapping("/{key}")
    @RequirePerm(resource = "page", action = PermAction.WRITE)
    public ResponseEntity<Void> remove(@PathVariable String key) {
        pageService.remove(key);
        return ResponseEntity.noContent().build();
    }
}
