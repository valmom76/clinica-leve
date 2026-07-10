# Clínica Leve — gestão completa para clínicas

Primeira fatia funcional da aplicação multiempresa para gestão de clínicas.

## Módulos deste marco

- autenticação JWT com clínica identificada por slug;
- isolamento de dados por clínica (tenant);
- perfis de acesso;
- pacientes;
- profissionais;
- especialidades;
- agendamentos em calendário;
- carga inicial de demonstração;
- MySQL, backend e frontend orquestrados por Docker Compose.

## Executar com Docker

1. Copie `env.example` para `.env`.
2. Troque as senhas e a chave JWT.
3. Execute:

```bash
docker compose up -d --build
```

4. Acesse `http://localhost:3000`.

Credenciais iniciais configuradas no `env.example`:

- clínica: `clinica-demo`
- e-mail: `admin@clinicaleve.local`
- senha: `Admin@123`

Altere esses dados antes de qualquer publicação.

## Estrutura

```text
platform/
├── backend/            Spring Boot + Java 21
├── frontend/           React + Vite + FullCalendar
├── docker-compose.yml
└── env.example
```

### Organização do frontend

```text
frontend/src/
├── app/                 composição da aplicação e navegação
├── auth/                autenticação e tela de login
├── components/
│   ├── layout/          sidebar e barra superior
│   └── ui/              componentes reutilizáveis
├── features/
│   ├── agenda/
│   ├── dashboard/
│   ├── patients/
│   ├── professionals/
│   └── shared/
├── hooks/               carregamento e estado dos dados
├── utils/               datas e constantes de domínio
├── api.ts               comunicação com a API
├── types.ts             contratos TypeScript
└── App.tsx              sessão e entrada da aplicação
```

## Produção na VPS

O frontend expõe a porta definida por `APP_PORT`. Na Integrator Host, configure
o OpenResty/Nginx principal para encaminhar o domínio para essa porta. O MySQL e
o backend ficam acessíveis somente na rede interna do Compose.

Para produção, configure também backup do volume `mysql_data`, HTTPS,
monitoramento e rotação dos segredos.
