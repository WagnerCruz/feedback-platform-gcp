-- Tabela principal de avaliações dos cursos.
CREATE TABLE avaliacao (
    id          BIGSERIAL PRIMARY KEY,
    descricao   VARCHAR(2000) NOT NULL,
    nota        SMALLINT      NOT NULL CHECK (nota BETWEEN 0 AND 10),
    urgencia    VARCHAR(10)   NOT NULL CHECK (urgencia IN ('CRITICA', 'MEDIA', 'BAIXA')),
    data_envio  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_avaliacao_data_envio ON avaliacao (data_envio);
CREATE INDEX idx_avaliacao_urgencia   ON avaliacao (urgencia);
