package br.com.orbithub.dataroom.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-145, KAN-63
 * Título: Data Room básico
 * Objetivo: Receber, listar, substituir e remover os documentos confidenciais da startup, organizados pelas categorias da
 *           lista gerenciável, e abrir o arquivo para quem já foi autorizado.
 * Decisões: Envia e remove o fundador administrador, o fundador e o colaborador com a seção Data Room. O tipo é conferido
 *           pelos primeiros bytes (e, nos formatos Office, pela extensão de um arquivo ZIP), no conjunto do parâmetro
 *           dataroom.formatos_aceitos, até dataroom.tamanho_max_mb. Substituir guarda a versão anterior, que deixa de ser a
 *           atual. Remover só esconde (o arquivo fica para o histórico). A categoria NDA guarda o NDA bilateral (KAN-63).
 *           Arquivos no armazenamento privado (disco local ou Azure Blob, pelo Adapter).
 */
@Service
public class DataRoomService {

    static final String TAMANHO = "dataroom.tamanho_max_mb";
    static final String FORMATOS = "dataroom.formatos_aceitos";
    static final String NDA = "NDA";
    static final Map<String, Set<String>> CONJUNTOS = Map.of(
            "PDF", Set.of("application/pdf"),
            "PDF_OFFICE", Set.of("application/pdf", Office.DOCX.tipo, Office.XLSX.tipo, Office.PPTX.tipo),
            "PDF_OFFICE_IMAGENS", Set.of("application/pdf", Office.DOCX.tipo, Office.XLSX.tipo, Office.PPTX.tipo, "image/png", "image/jpeg"));

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Formatos Office
     * Objetivo: Ligar a extensão do arquivo ZIP ao tipo Office.
     * Decisões: Word, Excel e PowerPoint.
     */
    enum Office {
        DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        final String extensao;
        final String tipo;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Office
     * Objetivo: Rotina auxiliar Office.
     * Decisões: Ver a documentação da classe.
     */
        Office(String extensao, String tipo) {
            this.extensao = extensao;
            this.tipo = tipo;
        }
    }

    private final JdbcTemplate jdbc;
    private final StartupsService startups;
    private final ListasService listas;
    private final ArmazenamentoArquivos arquivos;
    private final LeituraDeParametros parametros;
    private final ConsultaDeContas contas;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Criação
     * Objetivo: Receber o JDBC, as startups, as listas, o armazenamento, os parâmetros, as contas e o relógio.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public DataRoomService(JdbcTemplate jdbc, StartupsService startups, ListasService listas, ArmazenamentoArquivos arquivos,
                           LeituraDeParametros parametros, ConsultaDeContas contas, Clock relogio) {
        this.jdbc = jdbc;
        this.startups = startups;
        this.listas = listas;
        this.arquivos = arquivos;
        this.parametros = parametros;
        this.contas = contas;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Documentos da equipe
     * Objetivo: Listar à equipe todos os documentos não removidos, atuais e anteriores.
     * Decisões: Qualquer membro da equipe.
     */
    @Transactional(readOnly = true)
    public List<Documento> daEquipe(UUID eu, UUID startupId) {
        startups.papel(eu, startupId);
        return jdbc.query(SELECT + " WHERE startup_id = ? AND removido_em IS NULL ORDER BY categoria, nome, versao DESC", mapa(), startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-32, KAN-145
     * Título: Documentos atuais
     * Objetivo: Listar os documentos atuais (fora o NDA) para quem tem o Nível 2 liberado.
     * Decisões: Quem chama já conferiu o acesso.
     */
    @Transactional(readOnly = true)
    public List<Documento> atuais(UUID startupId) {
        return jdbc.query(SELECT + " WHERE startup_id = ? AND removido_em IS NULL AND atual AND categoria <> 'NDA' ORDER BY categoria, nome",
                mapa(), startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-63
     * Título: NDA da startup
     * Objetivo: Devolver o NDA bilateral atual da startup, se houver.
     * Decisões: Categoria NDA.
     */
    @Transactional(readOnly = true)
    public Optional<Documento> nda(UUID startupId) {
        return jdbc.query(SELECT + " WHERE startup_id = ? AND removido_em IS NULL AND atual AND categoria = 'NDA' ORDER BY enviado_em DESC",
                mapa(), startupId).stream().findFirst();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Enviar documento
     * Objetivo: Guardar um documento novo, na categoria escolhida.
     * Decisões: Nome obrigatório (até 160), descrição até 300.
     */
    @Transactional
    public Documento enviar(UUID eu, UUID startupId, String categoria, String nome, String descricao, String nomeArquivo, byte[] conteudo) {
        startups.exigirEditor(eu, startupId, SecaoDaStartup.DATA_ROOM);
        String cat = listas.exigirAtivo(Lista.CATEGORIA_DATAROOM, categoria).getCodigo();
        return gravar(eu, startupId, cat, nome, descricao, nomeArquivo, conteudo, 1, null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Substituir documento
     * Objetivo: Enviar a nova versão de um documento, mantendo a anterior.
     * Decisões: Mesma categoria e nome; a versão sobe um.
     */
    @Transactional
    public Documento substituir(UUID eu, UUID startupId, UUID documentoId, String nomeArquivo, byte[] conteudo) {
        startups.exigirEditor(eu, startupId, SecaoDaStartup.DATA_ROOM);
        Documento anterior = documento(startupId, documentoId);
        if (!anterior.atual()) {
            throw new RegraDeNegocioException("versao_antiga", "Substitua a versão atual do documento.");
        }
        jdbc.update("UPDATE dataroom_documento SET atual = FALSE WHERE id = ?", documentoId);
        UUID origem = anterior.origemId() == null ? anterior.id() : anterior.origemId();
        return gravar(eu, startupId, anterior.categoria(), anterior.nome(), anterior.descricao(), nomeArquivo, conteudo, anterior.versao() + 1, origem);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Remover documento
     * Objetivo: Tirar o documento (e as versões dele) do Data Room.
     * Decisões: Só esconde; o histórico de acessos continua apontando para ele.
     */
    @Transactional
    public void remover(UUID eu, UUID startupId, UUID documentoId) {
        startups.exigirEditor(eu, startupId, SecaoDaStartup.DATA_ROOM);
        Documento d = documento(startupId, documentoId);
        UUID origem = d.origemId() == null ? d.id() : d.origemId();
        jdbc.update("UPDATE dataroom_documento SET removido_em = ? WHERE startup_id = ? AND (id = ? OR origem_id = ?) AND removido_em IS NULL",
                Timestamp.from(relogio.instant()), startupId, origem, origem);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Documento
     * Objetivo: Ler o documento não removido da startup.
     * Decisões: 404 se não existir.
     */
    @Transactional(readOnly = true)
    public Documento documento(UUID startupId, UUID documentoId) {
        return jdbc.query(SELECT + " WHERE startup_id = ? AND id = ? AND removido_em IS NULL", mapa(), startupId, documentoId).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Abrir arquivo
     * Objetivo: Abrir o conteúdo do documento no armazenamento.
     * Decisões: Quem chama já conferiu a permissão.
     */
    public Arquivo abrir(Documento d) {
        return new Arquivo(arquivos.ler(d.chave()), d.tipo(), d.nome());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Gravar documento
     * Objetivo: Conferir tamanho e formato, guardar o arquivo e registrar o documento.
     * Decisões: Chave sem o nome original.
     */
    @SuppressWarnings("java:S107")
    private Documento gravar(UUID eu, UUID startupId, String categoria, String nome, String descricao, String nomeArquivo, byte[] conteudo,
                             int versao, UUID origem) {
        String n = nome == null || nome.isBlank() ? null : nome.strip();
        if (n == null || n.length() > 160) {
            throw new RegraDeNegocioException("nome_invalido", "Dê um nome ao documento (até 160 caracteres).");
        }
        String d = descricao == null || descricao.isBlank() ? null : descricao.strip();
        if (d != null && d.length() > 300) {
            throw new RegraDeNegocioException("descricao_longa", "A descrição pode ter até 300 caracteres.");
        }
        long maximo = parametros.inteiro(TAMANHO) * 1024 * 1024;
        if (conteudo == null || conteudo.length == 0 || conteudo.length > maximo) {
            throw new RegraDeNegocioException("documento_tamanho", "O documento precisa ter até " + parametros.inteiro(TAMANHO) + " MB.");
        }
        String tipo = formato(conteudo, nomeArquivo).filter(CONJUNTOS.getOrDefault(parametros.opcao(FORMATOS), Set.of())::contains)
                .orElseThrow(() -> new RegraDeNegocioException("documento_formato", "Formato não aceito no Data Room."));
        UUID id = UUID.randomUUID();
        String chave = "dataroom/" + startupId + "/" + id;
        arquivos.gravar(chave, new ByteArrayInputStream(conteudo), conteudo.length, tipo);
        jdbc.update("INSERT INTO dataroom_documento (id, startup_id, categoria, nome, descricao, chave, tipo, tamanho, versao, origem_id, atual, "
                + "enviado_por, enviado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)", id, startupId, categoria, n, d, chave, tipo,
                (long) conteudo.length, versao, origem, eu, Timestamp.from(relogio.instant()));
        return documento(startupId, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Formato pelo conteúdo
     * Objetivo: Descobrir o tipo real do arquivo pelos primeiros bytes.
     * Decisões: PDF, PNG, JPEG; ZIP só vale como Office com a extensão certa.
     */
    static Optional<String> formato(byte[] b, String nomeArquivo) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return Optional.of("application/pdf");
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of("image/png");
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (b.length >= 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4 && nomeArquivo != null) {
            String ext = nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            for (Office o : Office.values()) {
                if (o.extensao.equals(ext)) {
                    return Optional.of(o.tipo);
                }
            }
        }
        return Optional.empty();
    }

    static final String SELECT = "SELECT id, startup_id, categoria, nome, descricao, chave, tipo, tamanho, versao, origem_id, atual, enviado_por, "
            + "enviado_em FROM dataroom_documento";

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Mapear documento
     * Objetivo: Montar o documento a partir da linha, com o rótulo da categoria e o nome de quem enviou.
     * Decisões: Um RowMapper por chamada.
     */
    private RowMapper<Documento> mapa() {
        return (rs, i) -> new Documento(rs.getObject("id", UUID.class), rs.getString("categoria"),
                listas.rotulo(Lista.CATEGORIA_DATAROOM, rs.getString("categoria")), rs.getString("nome"), rs.getString("descricao"),
                rs.getString("chave"), rs.getString("tipo"), rs.getLong("tamanho"), rs.getInt("versao"), rs.getObject("origem_id", UUID.class),
                rs.getBoolean("atual"), contas.nome(rs.getObject("enviado_por", UUID.class)).orElse(null), rs.getTimestamp("enviado_em").toInstant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Documento do Data Room
     * Objetivo: Levar os dados do documento.
     * Decisões: A chave do armazenamento nunca sai na API (o controlador não a expõe).
     */
    public record Documento(UUID id, String categoria, String categoriaRotulo, String nome, String descricao, String chave, String tipo,
                            long tamanho, int versao, UUID origemId, boolean atual, String enviadoPor, Instant enviadoEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Arquivo
     * Objetivo: Rotina auxiliar Arquivo.
     * Decisões: Ver a documentação da classe.
     */
    public record Arquivo(InputStream conteudo, String tipo, String nome) {
    }
}
