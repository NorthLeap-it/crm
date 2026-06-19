package it.northleap.backend.services;

import it.northleap.backend.dtos.CreatePageDto;
import it.northleap.backend.dtos.UpdatePageDto;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.Page;
import it.northleap.backend.exceptions.BadRequestException;
import it.northleap.backend.exceptions.NotFoundException;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Porting di PagesService (pages.module.ts).
@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;
    private final ObjectTypeRepository objectTypeRepository;

    public List<Page> list() {
        return pageRepository.findAllByOrderByLabelAsc();
    }

    @Transactional
    public Page get(String key) {
        return pageOrThrow(key);
    }

    @Transactional
    public Page create(CreatePageDto dto) {
        Page page = new Page();
        page.setKey(dto.getKey());
        page.setLabel(dto.getLabel());
        page.setType(dto.getType());
        page.setLayout(dto.getLayout());
        if (dto.getObjectTypeId() != null) {
            ObjectType obj = objectTypeRepository.findById(dto.getObjectTypeId())
                    .orElseThrow(() -> new NotFoundException("Object type non trovato"));
            page.setObjectType(obj);
        }
        pageRepository.save(page);
        return page;
    }

    @Transactional
    public Page update(String key, UpdatePageDto dto) {
        Page page = pageOrThrow(key);
        if (dto.getLabel() != null) page.setLabel(dto.getLabel());
        if (dto.getLayout() != null) page.setLayout(dto.getLayout());
        pageRepository.save(page);
        return page;
    }

    @Transactional
    public void remove(String key) {
        Page page = pageOrThrow(key);
        // Nota: l'originale lancia NotFoundException anche qui per le pagine di sistema (un
        // probabile refuso, riusa l'unica eccezione importata nel file). Qui si usa
        // BadRequestException (400) per coerenza con ObjectsService.remove, che gestisce lo
        // stesso caso isSystem allo stesso modo — fix di coerenza interna deliberato.
        if (page.isSystem()) {
            throw new BadRequestException("Pagina di sistema non eliminabile");
        }
        pageRepository.delete(page);
    }

    private Page pageOrThrow(String key) {
        return pageRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException("Pagina non trovata"));
    }
}
