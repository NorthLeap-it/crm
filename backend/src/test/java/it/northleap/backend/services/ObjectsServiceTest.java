package it.northleap.backend.services;

import it.northleap.backend.dtos.FieldDefDto;
import it.northleap.backend.entities.FieldDef;
import it.northleap.backend.entities.ObjectType;
import it.northleap.backend.exceptions.BadRequestException;
import it.northleap.backend.repositories.FieldDefRepository;
import it.northleap.backend.repositories.ObjectTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectsServiceTest {

    @Mock private ObjectTypeRepository objectTypeRepository;
    @Mock private FieldDefRepository fieldDefRepository;
    @Mock private PageGeneratorService pageGeneratorService;

    private ObjectsService newService() {
        return new ObjectsService(objectTypeRepository, fieldDefRepository, pageGeneratorService);
    }

    private ObjectType objectType() {
        ObjectType obj = new ObjectType();
        obj.setId(UUID.randomUUID());
        obj.setKey("pipeline");
        return obj;
    }

    private FieldDef field(boolean required) {
        FieldDef f = new FieldDef();
        f.setId(UUID.randomUUID());
        f.setKey("name");
        f.setRequired(required);
        return f;
    }

    @Test
    void removeFieldThrowsWhenRequired() {
        when(objectTypeRepository.findByKey("pipeline")).thenReturn(Optional.of(objectType()));
        when(fieldDefRepository.findByObjectType_IdAndKey(any(), eq("name"))).thenReturn(Optional.of(field(true)));

        assertThrows(BadRequestException.class, () -> newService().removeField("pipeline", "name"));
        verify(fieldDefRepository, never()).delete(any());
    }

    @Test
    void updateFieldThrowsWhenRequired() {
        when(objectTypeRepository.findByKey("pipeline")).thenReturn(Optional.of(objectType()));
        when(fieldDefRepository.findByObjectType_IdAndKey(any(), eq("name"))).thenReturn(Optional.of(field(true)));

        assertThrows(BadRequestException.class, () -> newService().updateField("pipeline", "name", new FieldDefDto()));
        verify(fieldDefRepository, never()).save(any());
    }

    @Test
    void removeFieldDeletesWhenNotRequired() {
        FieldDef f = field(false);
        when(objectTypeRepository.findByKey("pipeline")).thenReturn(Optional.of(objectType()));
        when(fieldDefRepository.findByObjectType_IdAndKey(any(), eq("name"))).thenReturn(Optional.of(f));

        newService().removeField("pipeline", "name");
        verify(fieldDefRepository).delete(f);
    }
}
