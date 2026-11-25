package br.com.orbithub.administracao.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Listas gerenciáveis
 * Objetivo: Enumerar as listas que o administrador mantém pelo painel (P15).
 * Decisões: O conjunto de listas é fixo no código (cada uma tem uma regra que a usa); os itens de cada lista são As listas das preferências complementares do investidor entraram com o KAN-19; a dos motivos
 * de decisão da verificação, com o KAN-83 (o grupo é a decisão a que o motivo pertence).
 *           dados, editáveis. "comGrupo" diz se os itens da lista se dividem em grupos (documentos por tipo de
 *           usuário, estados por região).
 */
public enum Lista {
    SEGMENTO("Segmentos econômicos", false),
    LOCALIZACAO("Localizações", true),
    TIPO_INVESTIDOR("Tipos de investidor", false),
    INSTRUMENTO("Instrumentos de investimento", false),
    ESTAGIO_RODADA("Estágios de rodada", false),
    MODELO_NEGOCIO("Modelos de negócio", false),
    MOTIVO_ENCERRAMENTO("Motivos de encerramento de conexão", false),
    MOTIVO_RECUSA("Motivos de recusa de interesse", false),
    CATEGORIA_DENUNCIA("Categorias de denúncia", false),
    CATEGORIA_CHAMADO("Categorias de chamado", false),
    DOCUMENTO_VERIFICACAO("Documentos de verificação por tipo de usuário", true),
    CHECKLIST_CURADORIA("Itens do checklist de curadoria", false),
    MODALIDADE_INVESTIMENTO("Modalidades de investimento", false),
    HORIZONTE_INVESTIMENTO("Horizontes de investimento", false),
    PERFIL_RISCO("Perfis de risco", false),
    EXPERIENCIA_INVESTIDOR("Experiência como investidor", false),
    INTERESSE_ESTRATEGICO("Interesses estratégicos", false),
    MOTIVO_DECISAO_VERIFICACAO("Motivos de decisão da verificação", true);

    private final String titulo;
    private final boolean comGrupo;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Criação da lista
     * Objetivo: Guardar o título e se a lista usa grupos.
     * Decisões: Título em português, para a tela.
     */
    Lista(String titulo, boolean comGrupo) {
        this.titulo = titulo;
        this.comGrupo = comGrupo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Título da lista
     * Objetivo: Devolver o nome da lista para a tela.
     * Decisões: Somente leitura.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Lista com grupos
     * Objetivo: Dizer se os itens se dividem em grupos.
     * Decisões: Somente leitura.
     */
    public boolean isComGrupo() {
        return comGrupo;
    }
}
