package it.northleap.backend.entities;

// OWN < TEAM < ALL in ampiezza; TEAM non ha ancora filtro dedicato (manca il concetto di "team"),
// si comporta come ALL finché non viene implementato
public enum PermScope {
    OWN,
    TEAM,
    ALL
}
