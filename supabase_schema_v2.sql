-- Migration: Upgrade schema to Version 2 (Gestão de Ocorrências V2)
-- Created: 2026-06-30

-- 1. ADD ADDRESS FIELDS TO OCORRENCIAS
ALTER TABLE ocorrencias ADD COLUMN IF NOT EXISTS rua VARCHAR(255);
ALTER TABLE ocorrencias ADD COLUMN IF NOT EXISTS numero VARCHAR(20);
ALTER TABLE ocorrencias ADD COLUMN IF NOT EXISTS bairro VARCHAR(100);
ALTER TABLE ocorrencias ADD COLUMN IF NOT EXISTS cidade VARCHAR(100);
ALTER TABLE ocorrencias ADD COLUMN IF NOT EXISTS uf VARCHAR(2);

-- 2. CREATE PESSOAS TABLE (DEDUPLICATED BY CPF)
CREATE TABLE IF NOT EXISTS pessoas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    nome_social VARCHAR(255),
    cpf VARCHAR(14) UNIQUE,
    rg VARCHAR(20),
    rg_orgao_emissor VARCHAR(50),
    rg_uf VARCHAR(2),
    nascimento DATE,
    naturalidade VARCHAR(100),
    nacionalidade VARCHAR(100),
    filiacao TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. CREATE DOCUMENTOS TABLE (LINKS TO PESSOAS AND OCORRENCIAS)
CREATE TABLE IF NOT EXISTS documentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    pessoa_id UUID REFERENCES pessoas(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL, -- CNH, CIN, RG, OAB, etc.
    numero VARCHAR(50),
    url_imagem VARCHAR(512),
    texto_ocr TEXT,
    dados_estruturados JSONB DEFAULT '{}'::jsonb,
    hash_arquivo VARCHAR(64),
    data_upload TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. UPDATE APOIO_OCORRENCIA FOR DYNAMIC SUPPORT DETAILS
ALTER TABLE apoio_ocorrencia ADD COLUMN IF NOT EXISTS viatura VARCHAR(100);
ALTER TABLE apoio_ocorrencia ADD COLUMN IF NOT EXISTS encarregado VARCHAR(255);

-- To support multiple vehicles/entries of the same supporting agency, we remove the composite PK constraint and add a UUID PK.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'apoio_ocorrencia' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        ALTER TABLE apoio_ocorrencia DROP CONSTRAINT apoio_ocorrencia_pkey;
    END IF;
END $$;

ALTER TABLE apoio_ocorrencia ADD COLUMN IF NOT EXISTS id UUID DEFAULT gen_random_uuid();
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'apoio_ocorrencia' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        ALTER TABLE apoio_ocorrencia ADD PRIMARY KEY (id);
    END IF;
END $$;

-- 5. UPGRADE VEICULOS_ENVOLVIDOS TABLE FOR CRLV OCR AND LINK TO OWNER
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS proprietario_id UUID REFERENCES pessoas(id) ON DELETE SET NULL;
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS renavam VARCHAR(30);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS monobloco VARCHAR(50);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS especie VARCHAR(50);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS tipo_veiculo VARCHAR(50);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS carroceria VARCHAR(50);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS marca VARCHAR(50);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS versao VARCHAR(100);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS ano_fabricacao INTEGER;
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS ano_modelo INTEGER;
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS categoria_veiculo VARCHAR(50);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS exercicio VARCHAR(20);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS url_crlv VARCHAR(512);
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS ocr_texto_crlv TEXT;
ALTER TABLE veiculos_envolvidos ADD COLUMN IF NOT EXISTS ocr_dados_estruturados JSONB DEFAULT '{}'::jsonb;

-- 6. LINK VITIMAS TO THE DEDUPLICATED PERSONS TABLE
ALTER TABLE vitimas ADD COLUMN IF NOT EXISTS pessoa_id UUID REFERENCES pessoas(id) ON DELETE SET NULL;

-- 7. ENABLE ROW LEVEL SECURITY FOR NEW TABLES
ALTER TABLE pessoas ENABLE ROW LEVEL SECURITY;
ALTER TABLE documentos ENABLE ROW LEVEL SECURITY;

-- 8. CREATE POLICIES FOR PESSOAS AND DOCUMENTOS
CREATE POLICY "Allow authenticated reads on pessoas" ON pessoas FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on pessoas" ON pessoas FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on pessoas" ON pessoas FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on pessoas" ON pessoas FOR DELETE TO authenticated USING (true);

CREATE POLICY "Allow authenticated reads on documentos" ON documentos FOR SELECT TO authenticated USING (true);
CREATE POLICY "Allow authenticated inserts on documentos" ON documentos FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Allow authenticated updates on documentos" ON documentos FOR UPDATE TO authenticated USING (true);
CREATE POLICY "Allow authenticated deletes on documentos" ON documentos FOR DELETE TO authenticated USING (true);
