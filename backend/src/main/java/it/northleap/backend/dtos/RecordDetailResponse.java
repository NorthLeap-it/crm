package it.northleap.backend.dtos;

import it.northleap.backend.entities.Record;
import it.northleap.backend.entities.RecordLink;

import java.util.List;

public record RecordDetailResponse(Record record, List<RecordLink> outgoing, List<RecordLink> incoming) {
}
