package it.northleap.backend.services;

import it.northleap.backend.entities.FieldDef;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.entities.Page;
import it.northleap.backend.entities.PageType;
import it.northleap.backend.repositories.ObjectTypeRepository;
import it.northleap.backend.repositories.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Porting di page-generator.service.ts: genera/aggiorna automaticamente le pagine list+detail
// di default quando un ObjectType viene creato o un suo FieldDef cambia.
@Service
@RequiredArgsConstructor
public class PageGeneratorService {

    private final ObjectTypeRepository objectTypeRepository;
    private final PageRepository pageRepository;

    // @Transactional necessario: chiamato anche da ApplicationRunner (ObjectTypeSeeder), fuori
    // da una transazione web/OSIV, e obj.getFields() è una collezione LAZY
    @Transactional
    public void generate(UUID objectTypeId) {
        ObjectType obj = objectTypeRepository.findById(objectTypeId).orElse(null);
        if (obj == null) {
            return;
        }
        List<String> columns = obj.getFields().stream().limit(5).map(FieldDef::getKey).toList();

        upsertPage(obj.getKey() + ":list", obj.getPluralLabel(), PageType.LIST, obj,
                Map.of("columns", columns));
        upsertPage(obj.getKey() + ":detail", obj.getLabel(), PageType.DETAIL, obj,
                Map.of("sections", List.of("fields", "relations", "timeline")));
    }

    private void upsertPage(String key, String label, PageType type, ObjectType obj, Map<String, Object> layout) {
        Page page = pageRepository.findByKey(key).orElseGet(Page::new);
        page.setKey(key);
        page.setLabel(label);
        page.setType(type);
        page.setObjectType(obj);
        page.setLayout(layout);
        page.setGenerated(true);
        pageRepository.save(page);
    }
}
