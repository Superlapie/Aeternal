# OSRS RSPS Monorepo

From repo root (`d:\CodingProjects\OSRSRSPS`), use:

```powershell
.\run server   # start server
.\run client   # start client
.\run all      # start server + client
.\run stop     # stop Gradle daemons and free RSPS ports
```

Notes:
- `.\run all` starts server first, then launches client.
- Server port/client target are both `43595` by default.
- Seeing `:game:run` in an executing state is normal while server is online.
