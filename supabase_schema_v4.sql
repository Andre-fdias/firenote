-- Migration: Schema Upgrade for V4 Rebranding and Architecture (V4 Master Catalog and decoupling)
-- Created: 2026-06-30

-- 1. UPDATE PESSOAS TABLE (Add new fields if not exist)
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS sexo VARCHAR(20);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS telefone VARCHAR(20);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS logradouro VARCHAR(255);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS numero VARCHAR(20);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS bairro VARCHAR(100);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS cidade VARCHAR(100);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS uf VARCHAR(2);
ALTER TABLE pessoas ADD COLUMN IF NOT EXISTS cep VARCHAR(10);

-- 2. CREATE PARTICIPANTES_OCORRENCIA TABLE
CREATE TABLE IF NOT EXISTS participantes_ocorrencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    pessoa_id UUID NOT NULL REFERENCES pessoas(id) ON DELETE CASCADE,
    tipo_participacao VARCHAR(50) NOT NULL, -- Condutor, Vítima, Proprietário, Solicitante, Testemunha, Comunicante, Responsável, Equipe
    observacao TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. CREATE MASTER VEICULOS TABLE
CREATE TABLE IF NOT EXISTS veiculos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    placa VARCHAR(20) UNIQUE NOT NULL,
    renavam VARCHAR(20),
    chassi VARCHAR(50),
    marca VARCHAR(100),
    modelo VARCHAR(100),
    versao VARCHAR(100),
    tipo VARCHAR(50),
    categoria VARCHAR(50),
    cor VARCHAR(50),
    ano_fabricacao INTEGER,
    ano_modelo INTEGER,
    proprietario_id UUID REFERENCES pessoas(id) ON DELETE SET NULL,
    status VARCHAR(50) DEFAULT 'Ativo',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. CREATE VEICULOS_OCORRENCIA TABLE (Relating occurrence, vehicle and driver)
CREATE TABLE IF NOT EXISTS veiculos_ocorrencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    veiculo_id UUID NOT NULL REFERENCES veiculos(id) ON DELETE CASCADE,
    condutor_id UUID REFERENCES pessoas(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. RENAME V3 VIATURAS TO VIATURAS_OCORRENCIA (For backward compatibility)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'viaturas') 
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'viaturas_ocorrencia') THEN
        ALTER TABLE viaturas RENAME TO viaturas_ocorrencia;
    END IF;
END $$;

-- 6. CREATE MASTER VIATURAS TABLE
CREATE TABLE IF NOT EXISTS viaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prefixo VARCHAR(50) UNIQUE NOT NULL,
    placa VARCHAR(20) UNIQUE,
    tipo VARCHAR(50) NOT NULL,
    marca VARCHAR(100),
    modelo VARCHAR(100),
    quartel VARCHAR(100),
    status VARCHAR(50) DEFAULT 'Ativo',
    capacidade INTEGER,
    equipamentos TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. ALTER VIATURAS_OCORRENCIA TO INCLUDE DISPATCH TIMES AND LINK TO MASTER VIATURAS
ALTER TABLE viaturas_ocorrencia ADD COLUMN IF NOT EXISTS viatura_id UUID REFERENCES viaturas(id) ON DELETE SET NULL;
ALTER TABLE viaturas_ocorrencia ADD COLUMN IF NOT EXISTS km_retorno INTEGER;
ALTER TABLE viaturas_ocorrencia ADD COLUMN IF NOT EXISTS hora_despacho TIMESTAMPTZ;
ALTER TABLE viaturas_ocorrencia ADD COLUMN IF NOT EXISTS hora_saida TIMESTAMPTZ;
ALTER TABLE viaturas_ocorrencia ADD COLUMN IF NOT EXISTS hora_chegada TIMESTAMPTZ;
ALTER TABLE viaturas_ocorrencia ADD COLUMN IF NOT EXISTS hora_retorno TIMESTAMPTZ;

-- Migrate existing prefix/tipo from viaturas_ocorrencia to master viaturas if needed
DO $$
DECLARE
    r RECORD;
    new_id UUID;
BEGIN
    FOR r IN SELECT DISTINCT prefixo, tipo FROM viaturas_ocorrencia WHERE viatura_id IS NULL LOOP
        new_id := gen_random_uuid();
        INSERT INTO viaturas (id, prefixo, tipo, status)
        VALUES (new_id, r.prefixo, r.tipo, 'Ativo')
        ON CONFLICT (prefixo) DO UPDATE SET tipo = r.tipo
        RETURNING id INTO new_id;
        
        UPDATE viaturas_ocorrencia SET viatura_id = new_id WHERE prefixo = r.prefixo;
    END LOOP;
END $$;

-- 8. RENAME V3 MILITARES TO MILITARES_VIATURA
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'militares') 
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'militares_viatura') THEN
        ALTER TABLE militares RENAME TO militares_viatura;
    END IF;
END $$;

-- 9. CREATE MASTER MILITARES TABLE
CREATE TABLE IF NOT EXISTS militares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    re VARCHAR(20) UNIQUE NOT NULL,
    nome VARCHAR(255) NOT NULL,
    nome_guerra VARCHAR(100) NOT NULL,
    graduacao VARCHAR(50) NOT NULL,
    funcao VARCHAR(100),
    lotacao VARCHAR(100),
    situacao VARCHAR(50) DEFAULT 'Ativo',
    telefone VARCHAR(20),
    email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. ALTER MILITARES_VIATURA TO LINK TO MASTER MILITARES AND VIATURAS_OCORRENCIA
ALTER TABLE militares_viatura RENAME COLUMN viatura_id TO viatura_ocorrencia_id;
ALTER TABLE militares_viatura ADD COLUMN IF NOT EXISTS militar_id UUID REFERENCES militares(id) ON DELETE SET NULL;

-- Migrate existing RE/Nome Guerra to master militares
DO $$
DECLARE
    m RECORD;
    new_m_id UUID;
BEGIN
    FOR m IN SELECT DISTINCT re, nome_guerra, graduacao, funcao FROM militares_viatura WHERE militar_id IS NULL LOOP
        new_m_id := gen_random_uuid();
        INSERT INTO militares (id, re, nome, nome_guerra, graduacao, funcao, situacao)
        VALUES (new_m_id, m.re, m.nome_guerra, m.nome_guerra, m.graduacao, m.funcao, 'Ativo')
        ON CONFLICT (re) DO UPDATE SET nome_guerra = m.nome_guerra
        RETURNING id INTO new_m_id;

        UPDATE militares_viatura SET militar_id = new_m_id WHERE re = m.re;
    END LOOP;
END $$;

-- 11. CREATE AVALIACAO_CLINICA TABLE
CREATE TABLE IF NOT EXISTS avaliacao_clinica (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vitima_id UUID NOT NULL REFERENCES vitimas(id) ON DELETE CASCADE,
    glasgow INTEGER,
    pressao VARCHAR(50),
    frequencia_cardiaca INTEGER,
    frequencia_respiratoria INTEGER,
    temperatura DOUBLE PRECISION,
    oximetria INTEGER,
    lesoes TEXT,
    hospital_destino VARCHAR(255),
    viatura_socorro UUID REFERENCES viaturas(id) ON DELETE SET NULL,
    resultado VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Migrate existing signs from vitimas to avaliacao_clinica
DO $$
DECLARE
    v RECORD;
BEGIN
    FOR v IN SELECT id, sinais_vitais, lesoes_aparentes, hospital_destino, viatura_socorro_id, resultado_ocorrencia FROM vitimas LOOP
        INSERT INTO avaliacao_clinica (
            vitima_id,
            glasgow,
            pressao,
            frequencia_cardiaca,
            frequencia_respiratoria,
            temperatura,
            oximetria,
            lesoes,
            hospital_destino,
            viatura_socorro,
            resultado
        ) VALUES (
            v.id,
            (v.sinais_vitais->>'escalaGCS')::integer,
            v.sinais_vitais->>'pressaoArterial',
            (v.sinais_vitais->>'pulso')::integer,
            NULL, -- respirator frequency was not saved in JSON
            (v.sinais_vitais->>'temperatura')::double precision,
            (v.sinais_vitais->>'saturacaoO2')::integer,
            v.lesoes_aparentes,
            v.hospital_destino,
            v.viatura_socorro_id,
            v.resultado_ocorrencia
        );
    END LOOP;
END $$;

-- 12. CREATE EVIDENCIAS TABLE
CREATE TABLE IF NOT EXISTS evidencias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL, -- Imagem, Vídeo, Áudio, Documento, OCR, Croqui
    hash_sha256 VARCHAR(64) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario VARCHAR(100),
    url_storage TEXT NOT NULL,
    miniatura_url TEXT,
    ocr_bruto TEXT,
    json_ocr JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. CREATE TIMELINE_OCORRENCIA TABLE
CREATE TABLE IF NOT EXISTS timeline_ocorrencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    evento VARCHAR(100) NOT NULL, -- Despacho, Chegada, Atendimento, Apoio, Hospital, Encerramento
    descricao TEXT,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. CREATE AUDIT_LOG TABLE
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocorrencia_id UUID REFERENCES ocorrencias(id) ON DELETE SET NULL,
    usuario VARCHAR(100) NOT NULL,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tabela_alterada VARCHAR(100) NOT NULL,
    campo_alterado VARCHAR(100) NOT NULL,
    valor_anterior TEXT,
    valor_novo TEXT
);

-- 15. ENABLE ROW LEVEL SECURITY
ALTER TABLE participantes_ocorrencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE veiculos ENABLE ROW LEVEL SECURITY;
ALTER TABLE veiculos_ocorrencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE viaturas ENABLE ROW LEVEL SECURITY;
ALTER TABLE viaturas_ocorrencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE militares ENABLE ROW LEVEL SECURITY;
ALTER TABLE militares_viatura ENABLE ROW LEVEL SECURITY;
ALTER TABLE avaliacao_clinica ENABLE ROW LEVEL SECURITY;
ALTER TABLE evidencias ENABLE ROW LEVEL SECURITY;
ALTER TABLE timeline_ocorrencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;

-- 16. CREATE RLS POLICIES FOR NEW TABLES
CREATE POLICY "Allow authenticated access to participantes_ocorrencia" ON participantes_ocorrencia FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to veiculos" ON veiculos FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to veiculos_ocorrencia" ON veiculos_ocorrencia FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to viaturas" ON viaturas FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to viaturas_ocorrencia" ON viaturas_ocorrencia FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to militares" ON militares FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to militares_viatura" ON militares_viatura FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to avaliacao_clinica" ON avaliacao_clinica FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to evidencias" ON evidencias FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to timeline_ocorrencia" ON timeline_ocorrencia FOR ALL TO authenticated USING (true) WITH CHECK (true);
CREATE POLICY "Allow authenticated access to audit_log" ON audit_log FOR ALL TO authenticated USING (true) WITH CHECK (true);
