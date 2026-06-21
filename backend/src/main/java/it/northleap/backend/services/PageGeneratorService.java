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
        generate(objectTypeId, false);
    }

    // isSystem=true va usato solo per gli ObjectType seminati al bootstrap (replica
    // l'isSystem:true che seed.ts assegna inline alle proprie pagine generate, distinto dal
    // page-generator.service.ts riutilizzabile che invece non lo imposta mai — qui i due casi
    // sono lo stesso metodo con un parametro, non duplicati come nell'originale)
    @Transactional
    public void generate(UUID objectTypeId, boolean isSystem) {
        ObjectType obj = objectTypeRepository.findById(objectTypeId).orElse(null);
        if (obj == null) {
            return;
        }
        List<String> columns = obj.getFields().stream().limit(5).map(FieldDef::getKey).toList();

        upsertPage(obj.getKey() + ":list", obj.getPluralLabel(), PageType.LIST, obj,
                Map.of("columns", columns), isSystem);
        upsertPage(obj.getKey() + ":detail", obj.getLabel(), PageType.DETAIL, obj,
                Map.of("sections", List.of("fields", "relations", "timeline")), isSystem);
    }

    private void upsertPage(String key, String label, PageType type, ObjectType obj, Map<String, Object> layout, boolean isSystem) {
        Page page = pageRepository.findByKey(key).orElseGet(Page::new);
        page.setKey(key);
        page.setLabel(label);
        page.setType(type);
        page.setObjectType(obj);
        page.setLayout(layout);
        page.setGenerated(true);
        page.setSystem(isSystem);
        pageRepository.save(page);
    }
}
