-- Migration: Initialize schema for FireNotes (Emergency/Police Occurrences MVP)
-- Created: 2026-06-29

-- Enable UUID extension if not enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. OCORRENCIAS TABLE
CREATE TABLE ocorrencias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    protocolo VARCHAR(50) UNIQUE NOT NULL,
    natureza VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    historico TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_natureza CHECK (natureza IN ('Incêndio', 'Salvamento', 'Acidente de Trânsito', 'Queda', 'Pessoal'))
);

-- 2. VEICULOS ENVOLVIDOS TABLE
CREATE TABLE veiculos_envolvidos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    placa VARCHAR(10),
    cor VARCHAR(30),
    chassi VARCHAR(50),
    modelo VARCHAR(100),
    ano INTEGER,
    dados_motorista JSONB DEFAULT '{}'::jsonb, -- Store driver details: CNH, name, status, birth date, OCR metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. VITIMAS TABLE
CREATE TABLE vitimas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    nome VARCHAR(255),
    idade INTEGER,
    lesoes_aparentes TEXT,
    destino_socorro VARCHAR(255),
    quem_socorreu VARCHAR(255),
    resultado_ocorrencia VARCHAR(100), -- E.g., 'Óbito', 'Estável', 'Recusa de Atendimento'
    sinais_vitais JSONB NOT NULL DEFAULT '{}'::jsonb, -- Store pulse, PA, saturation, temperature, etc.
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. ORGAOS DE APOIO TABLE
CREATE TABLE orgaos_apoio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    sigla VARCHAR(20) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. APOIO OCORRENCIA TABLE (N:N intermediate table)
CREATE TABLE apoio_ocorrencia (
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    orgao_id UUID NOT NULL REFERENCES orgaos_apoio(id) ON DELETE CASCADE,
    PRIMARY KEY (ocorrencia_id, orgao_id)
);

-- TRIGGERS & FUNCTIONS FOR updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_ocorrencias_updated_at
    BEFORE UPDATE ON ocorrencias
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- INDEXES FOR PERFORMANCE OPTIMIZATION
CREATE INDEX idx_ocorrencias_protocolo ON ocorrencias(protocolo);
CREATE INDEX idx_ocorrencias_natureza ON ocorrencias(natureza);
CREATE INDEX idx_ocorrencias_data_hora ON ocorrencias(data_hora);
CREATE INDEX idx_veiculos_ocorrencia_id ON veiculos_envolvidos(ocorrencia_id);
CREATE INDEX idx_vitimas_ocorrencia_id ON vitimas(ocorrencia_id);
CREATE INDEX idx_apoio_ocorrencia_orgao ON apoio_ocorrencia(orgao_id);

-- ENABLE ROW LEVEL SECURITY (RLS) FOR SUPABASE SECURITY
ALTER TABLE ocorrencias ENABLE ROW LEVEL SECURITY;
ALTER TABLE veiculos_envolvidos ENABLE ROW LEVEL SECURITY;
ALTER TABLE vitimas ENABLE ROW LEVEL SECURITY;
ALTER TABLE orgaos_apoio ENABLE ROW LEVEL SECURITY;
ALTER TABLE apoio_ocorrencia ENABLE ROW LEVEL SECURITY;

-- CREATE POLICIES (Assuming MVP operates with authenticated dashboard users or public anon during field testing)
-- For this MVP, we will allow all authenticated users full access.
CREATE POLICY "Allow authenticated reads on ocorrencias" ON ocorrencias 
    FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on ocorrencias" ON ocorrencias 
    FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on ocorrencias" ON ocorrencias 
    FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on ocorrencias" ON ocorrencias 
    FOR DELETE TO authenticated USING (true);

-- Repeat for veiculos_envolvidos
CREATE POLICY "Allow authenticated reads on veiculos_envolvidos" ON veiculos_envolvidos 
    FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on veiculos_envolvidos" ON veiculos_envolvidos 
    FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on veiculos_envolvidos" ON veiculos_envolvidos 
    FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on veiculos_envolvidos" ON veiculos_envolvidos 
    FOR DELETE TO authenticated USING (true);

-- Repeat for vitimas
CREATE POLICY "Allow authenticated reads on vitimas" ON vitimas 
    FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on vitimas" ON vitimas 
    FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on vitimas" ON vitimas 
    FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on vitimas" ON vitimas 
    FOR DELETE TO authenticated USING (true);

-- Repeat for orgaos_apoio
CREATE POLICY "Allow authenticated reads on orgaos_apoio" ON orgaos_apoio 
    FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on orgaos_apoio" ON orgaos_apoio 
    FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on orgaos_apoio" ON orgaos_apoio 
    FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on orgaos_apoio" ON orgaos_apoio 
    FOR DELETE TO authenticated USING (true);

-- Repeat for apoio_ocorrencia
CREATE POLICY "Allow authenticated reads on apoio_ocorrencia" ON apoio_ocorrencia 
    FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on apoio_ocorrencia" ON apoio_ocorrencia 
    FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on apoio_ocorrencia" ON apoio_ocorrencia 
    FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on apoio_ocorrencia" ON apoio_ocorrencia 
    FOR DELETE TO authenticated USING (true);

-- PRE-POPULATE ORGAOS DE APOIO WITH BASIC ORGANIZATIONS
INSERT INTO orgaos_apoio (nome, sigla) VALUES
('Polícia Rodoviária Federal', 'PRF'),
('Corpo de Bombeiros Militar', 'CBM'),
('Polícia Militar', 'PM'),
('Serviço de Atendimento Móvel de Urgência', 'SAMU'),
('Defesa Civil', 'DC')
ON CONFLICT (sigla) DO NOTHING;
