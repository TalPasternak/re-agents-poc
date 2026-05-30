# Requirements Model

The canonical source of truth for the requirements model. The SRS in
`output/` is generated from these files.

## Layout

```
model/
├── components/      # one .yaml per Component
├── capabilities/    # one .yaml per Capability
├── requirements/    # one .yaml per Requirement
├── interfaces/      # one .yaml per Interface
├── processes/       # one .yaml per Process
└── relationships/   # one .yaml per Relationship
```

The metamodel is documented in `.claude/skills/metamodel/SKILL.md` and
mirrors the Python `SystemMetaModel.py` in the parent project.

## ID convention

Stable slug-style IDs, prefixed by entity kind:

- `c-...` for Component
- `cap-...` for Capability
- `req-...` for Requirement
- `if-...` for Interface
- `proc-...` for Process
- `rel-...` for Relationship
