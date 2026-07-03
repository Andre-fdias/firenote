-- FIRE NOTES V5.3 DATABASE MIGRATION
-- ADD INDEXES FOR PERFORMANCE OPTIMIZATION OF GLOBAL SEARCH AND FILTERS

-- Indexes for Ocorrencias search
CREATE INDEX IF NOT EXISTS idx_ocorrencias_protocolo ON ocorrencias(protocolo);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_natureza ON ocorrencias(natureza);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_data_hora ON ocorrencias(data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_cidade ON ocorrencias(cidade);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_bairro ON ocorrencias(bairro);

-- Indexes for Veiculos search
CREATE INDEX IF NOT EXISTS idx_veiculos_ocorrencia_placa ON veiculos_ocorrencia(placa);

-- Indexes for Pessoas/Documentos search
CREATE INDEX IF NOT EXISTS idx_pessoas_cpf ON pessoas(cpf);
CREATE INDEX IF NOT EXISTS idx_pessoas_nome ON pessoas(nome);

-- Indexes for Viaturas search
CREATE INDEX IF NOT EXISTS idx_viaturas_ocorrencia_prefixo ON viaturas_ocorrencia(prefixo);

-- Indexes for Militares search
CREATE INDEX IF NOT EXISTS idx_militares_viatura_re ON militares_viatura(re);
CREATE INDEX IF NOT EXISTS idx_militares_viatura_nome_guerra ON militares_viatura(nome_guerra);

-- Indexes for Vitimas search
CREATE INDEX IF NOT EXISTS idx_vitimas_pessoa_id ON vitimas(pessoa_id);
CREATE INDEX IF NOT EXISTS idx_vitimas_nome ON vitimas(nome);
