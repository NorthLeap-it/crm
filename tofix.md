1. services/RecordQueryService.java — è il pezzo più delicato: costruisce SQL nativo per i filtri dinamici su jsonb. Ho validato i nomi campo con whitelist regex e parametrizzato tutti i valori, ma è codice che genera SQL a   
  mano — merita una lettura attenta tua, è la superficie più sensibile a injection.                                                                                                                                                 
  2. services/RecordValidator.java — tutta la logica di business per ogni FieldType (coercizioni, validazioni). Ha test unitari per tutti i casi principali, ma vale la pena controllare che le regole corrispondano a quello che   
  vuoi davvero (es. i bound numerici, il parsing permissivo delle date).                                                                                                                                                            
  3. Sezione "Jackson note" in CLAUDE.md — è una scelta architetturale non banale (niente JsonNode, solo Object/Map/List per le colonne JSON) dovuta a un conflitto reale tra Jackson 2 e 3 in Spring Boot 4.1 + Hibernate 7.2. Vale
  la pena capirla, perché ogni futura colonna JSON dovrà seguire la stessa regola.                                                                                                                                                  
  4. RecordsService.java — nota che l'RBAC qui NON passa da @RequirePerm ma chiama RbacService.resolve(...) direttamente, perché la risorsa è il {key} dinamico nell'URL. È fedele all'originale ma è un'eccezione al pattern usato 
  altrove, buono da avere presente.                                                                  Jump to bottom (ctrl+End) ↓ 





- RecordQueryService: ho verificato live solo un filtro semplice (amount gt 1000). Operatori come between/in/nin/contains e gruppi AND/OR annidati su più livelli non hanno test automatici né sono stati provati live — se vuoi più sicurezza qui, posso scrivere unit test dedicati.
- RecordLink: l'entity e il repository esistono, ma nessun endpoint crea ancora righe RecordLink — un campo RELATION su un record oggi salva solo l'ID grezzo dentro data, non crea un collegamento vero in record_link. Coerente con l'originale (la creazione dei link probabilmente arriva da un meccanismo successivo, forse le azioni del workflow engine), ma è un buco funzionale da tenere a mente.
- Bulk update (solo bulk delete testato live).
