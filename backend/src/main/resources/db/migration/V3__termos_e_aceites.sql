-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 00:40
-- Cartão Jira: KAN-92
-- Título: Termos, políticas e avisos versionados
-- Objetivo: Guardar cada versão publicada dos documentos legais e dos avisos e o registro de quem
--           aceitou qual versão e quando.
-- Decisões: Versão sequencial por tipo; a vigente é a de maior versão com vigência já iniciada.
--           Versões antigas nunca são apagadas (continuam consultáveis). As versões 1 abaixo são
--           TEXTOS PROVISÓRIOS, a serem substituídos pela redação do jurídico antes do lançamento.

CREATE TABLE documento_legal (
    id             UUID         PRIMARY KEY,
    tipo           VARCHAR(40)  NOT NULL,
    versao         INTEGER      NOT NULL,
    titulo         VARCHAR(160) NOT NULL,
    conteudo       TEXT         NOT NULL,
    vigente_desde  TIMESTAMPTZ  NOT NULL,
    publicado_em   TIMESTAMPTZ  NOT NULL,
    publicado_por  UUID,
    CONSTRAINT uk_documento_legal_tipo_versao UNIQUE (tipo, versao)
);

CREATE TABLE aceite_documento (
    id           UUID        PRIMARY KEY,
    usuario_id   UUID        NOT NULL,
    documento_id UUID        NOT NULL REFERENCES documento_legal (id),
    aceito_em    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_aceite_usuario_documento UNIQUE (usuario_id, documento_id)
);

CREATE INDEX ix_aceite_documento_usuario ON aceite_documento (usuario_id);

INSERT INTO documento_legal (id, tipo, versao, titulo, conteudo, vigente_desde, publicado_em) VALUES
('6f1d0a52-0b7e-4c1f-9a43-000000000001', 'TERMOS_DE_USO', 1, 'Termos de Uso',
'[TEXTO PROVISÓRIO — aguardando redação do jurídico]

1. O Orbit Hub é uma plataforma de conexão entre investidores e empreendedores. A plataforma não intermedeia ofertas de valores mobiliários, não recebe recursos de investimento e não faz recomendação de investimento.
2. Para usar a plataforma, você precisa ter 18 anos ou mais, informar dados verdadeiros e manter sua senha em sigilo.
3. Você é responsável pelas informações que publica. Informações de startups são declaradas pelos empreendedores e não são auditadas pela plataforma.
4. É proibido usar a plataforma para fraudes, assédio, divulgação de informação confidencial de terceiros ou qualquer atividade ilegal. Violações podem levar à suspensão ou exclusão da conta, conforme a Política de Moderação.
5. Estes termos podem mudar. Quando uma nova versão entrar em vigor, pediremos o seu aceite no próximo acesso.',
'2026-11-24T00:00:00Z', '2026-11-24T00:00:00Z'),

('6f1d0a52-0b7e-4c1f-9a43-000000000002', 'POLITICA_DE_PRIVACIDADE', 1, 'Política de Privacidade',
'[TEXTO PROVISÓRIO — aguardando redação do jurídico e do encarregado de dados (DPO)]

1. Tratamos seus dados pessoais para criar e manter sua conta, conectar investidores e empreendedores e cumprir obrigações legais, com base na Lei Geral de Proteção de Dados (Lei 13.709/2018).
2. Coletamos nome, e-mail, papéis na plataforma e os dados que você decidir publicar. Senhas são guardadas apenas como hash.
3. Informações de nível 2 (confidenciais) de uma startup só são liberadas a investidores autorizados pelo empreendedor, com termo de confidencialidade aceito.
4. Você pode pedir acesso, correção, portabilidade ou exclusão dos seus dados pelo painel da conta.
5. Registramos quem aceitou qual versão desta política e quando.',
'2026-11-24T00:00:00Z', '2026-11-24T00:00:00Z'),

('6f1d0a52-0b7e-4c1f-9a43-000000000003', 'TERMO_DE_CONFIDENCIALIDADE', 1, 'Termo de Confidencialidade da Plataforma',
'[TEXTO PROVISÓRIO — aguardando redação e validação jurídica (KAN-63)]

1. Ao acessar informações de nível 2 de uma startup, você se compromete a usá-las apenas para avaliar uma possível relação de investimento.
2. Você não pode copiar, divulgar ou compartilhar essas informações com terceiros sem autorização expressa do empreendedor.
3. Os acessos são registrados. O descumprimento deste termo leva à suspensão imediata do acesso e pode gerar responsabilização.
4. Este compromisso continua valendo mesmo depois que o acesso for revogado ou a conta for encerrada.',
'2026-11-24T00:00:00Z', '2026-11-24T00:00:00Z'),

('6f1d0a52-0b7e-4c1f-9a43-000000000004', 'POLITICA_DE_MODERACAO', 1, 'Política de Moderação',
'[TEXTO PROVISÓRIO — aguardando redação do jurídico]

1. Conteúdos e condutas podem ser denunciados por qualquer usuário. A equipe analisa cada denúncia dentro dos prazos definidos.
2. As sanções vão de aviso a suspensão temporária e exclusão da conta, conforme a gravidade e a reincidência.
3. Toda sanção pode ser contestada por recurso, analisado por outra pessoa da equipe.',
'2026-11-24T00:00:00Z', '2026-11-24T00:00:00Z'),

('6f1d0a52-0b7e-4c1f-9a43-000000000005', 'AVISO_NAO_COMPROMISSO', 1, 'Aviso: não representa compromisso de investimento',
'Demonstrar interesse, conversar ou se conectar pela plataforma não representa compromisso de investimento de nenhuma das partes. O Orbit Hub não faz recomendação de investimento.',
'2026-11-24T00:00:00Z', '2026-11-24T00:00:00Z'),

('6f1d0a52-0b7e-4c1f-9a43-000000000006', 'AVISO_DADOS_DECLARADOS', 1, 'Aviso: dados declarados, não auditados',
'As informações desta startup foram declaradas pelos próprios empreendedores e não foram auditadas pelo Orbit Hub.',
'2026-11-24T00:00:00Z', '2026-11-24T00:00:00Z');
