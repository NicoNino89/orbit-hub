package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.empreendimentos.domain.Fundador;
import br.com.orbithub.empreendimentos.domain.MembroDaStartup;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.RedeSocial;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.domain.Startup.Cadastro;
import br.com.orbithub.empreendimentos.repository.MembroDaStartupRepository;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Cadastro das startups
 * Objetivo: Criar a startup, ler e editar os dados cadastrais e o logotipo, e listar as startups de quem pergunta.
 * Decisões: Quem cria vira fundador administrador. Quem não é membro ativo recebe 404. Nome, segmento e localização são
 *           obrigatórios; CNPJ é opcional (dígitos conferidos e único). A situação cadastral na Receita é conferida na
 *           verificação da startup (KAN-62), pelo analista. A participação dos fundadores é o cap table (Nível 2): só a
 *           equipe vê. O logotipo é PNG, JPEG ou WebP (conferido pelos bytes), até startup.logotipo_max_mb.
 */
@Service
public class StartupsService {

    static final String LOGOTIPO_MAX = "startup.logotipo_max_mb";
    private static final Pattern URL = Pattern.compile("^https?://[^\\s]+$");

    private final StartupRepository startups;
    private final MembroDaStartupRepository membros;
    private final ListasService listas;
    private final ArmazenamentoArquivos arquivos;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, listas, armazenamento, parâmetros e relógio.
     * Decisões: Injeção por construtor.
     */
    public StartupsService(StartupRepository startups, MembroDaStartupRepository membros, ListasService listas,
                           ArmazenamentoArquivos arquivos, LeituraDeParametros parametros, Clock relogio) {
        this.startups = startups;
        this.membros = membros;
        this.listas = listas;
        this.arquivos = arquivos;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Criar startup
     * Objetivo: Cadastrar a startup em rascunho e tornar quem cria o fundador administrador.
     * Decisões: Mesmas validações da edição.
     */
    @Transactional
    public Startup criar(UUID eu, Cadastro pedido) {
        Instant agora = relogio.instant();
        Startup startup = Startup.nova(conferir(pedido, null), eu, agora);
        startups.save(startup);
        membros.save(MembroDaStartup.novo(startup.getId(), eu, PapelNaStartup.FUNDADOR_ADMINISTRADOR, agora));
        return startup;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Minhas startups
     * Objetivo: Listar as startups de que a pessoa faz parte, com o papel dela.
     * Decisões: Só vínculos ativos.
     */
    @Transactional(readOnly = true)
    public List<Vinculo> minhas(UUID eu) {
        List<MembroDaStartup> vinculos = membros.findByUsuarioIdAndSaiuEmIsNullOrderByEntrouEm(eu);
        Map<UUID, Startup> porId = startups.findAllById(vinculos.stream().map(MembroDaStartup::getStartupId).toList()).stream()
                .collect(Collectors.toMap(Startup::getId, s -> s));
        return vinculos.stream().map(v -> new Vinculo(porId.get(v.getStartupId()), v.getPapel())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Detalhe
     * Objetivo: Devolver a startup e o papel de quem pergunta.
     * Decisões: Quem não é membro recebe 404.
     */
    @Transactional(readOnly = true)
    public Vinculo detalhe(UUID eu, UUID startupId) {
        return new Vinculo(startup(startupId), papel(eu, startupId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Editar cadastro
     * Objetivo: Salvar os dados cadastrais, as redes e os fundadores.
     * Decisões: Fundadores editam; colaborador só com a seção de dados cadastrais autorizada (KAN-65).
     */
    @Transactional
    public Startup editarCadastro(UUID eu, UUID startupId, Cadastro pedido) {
        exigirEditor(eu, startupId, SecaoDaStartup.CADASTRO);
        Startup startup = startup(startupId);
        startup.atualizarCadastro(conferir(pedido, startupId), relogio.instant());
        return startup;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Trocar logotipo
     * Objetivo: Guardar o logotipo novo e apagar o anterior.
     * Decisões: PNG, JPEG ou WebP, até startup.logotipo_max_mb.
     */
    @Transactional
    public Startup trocarLogotipo(UUID eu, UUID startupId, byte[] conteudo) {
        exigirEditor(eu, startupId, SecaoDaStartup.CADASTRO);
        long maximo = parametros.inteiro(LOGOTIPO_MAX) * 1024 * 1024;
        if (conteudo == null || conteudo.length == 0 || conteudo.length > maximo) {
            throw new RegraDeNegocioException("logotipo_tamanho", "O logotipo precisa ter até " + parametros.inteiro(LOGOTIPO_MAX) + " MB.");
        }
        String tipo = formatoDaImagem(conteudo).orElseThrow(() -> new RegraDeNegocioException("logotipo_formato",
                "Envie o logotipo em PNG, JPG ou WebP."));
        Startup startup = startup(startupId);
        String chave = "startups/" + startupId + "/logotipo-" + UUID.randomUUID() + "." + tipo.substring(tipo.indexOf('/') + 1);
        arquivos.gravar(chave, new ByteArrayInputStream(conteudo), conteudo.length, tipo);
        String antiga = startup.trocarLogotipo(chave, tipo, relogio.instant());
        if (antiga != null) {
            arquivos.remover(antiga);
        }
        return startup;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Ler logotipo
     * Objetivo: Devolver o logotipo para a equipe.
     * Decisões: O catálogo (S6) terá a própria rota pública; aqui só membros.
     */
    @Transactional(readOnly = true)
    public Logotipo lerLogotipo(UUID eu, UUID startupId) {
        papel(eu, startupId);
        Startup startup = startup(startupId);
        if (startup.getLogotipoChave() == null) {
            throw new RecursoNaoEncontradoException("A startup ainda não tem logotipo.");
        }
        return new Logotipo(arquivos.ler(startup.getLogotipoChave()), startup.getLogotipoTipo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Papel de quem pergunta
     * Objetivo: Exigir vínculo ativo com a startup e devolver o papel.
     * Decisões: Sem vínculo: 404, sem revelar se a startup existe. Serviço público para os módulos que agem pela startup.
     */
    @Transactional(readOnly = true)
    public PapelNaStartup papel(UUID eu, UUID startupId) {
        return membros.findByStartupIdAndUsuarioIdAndSaiuEmIsNull(startupId, eu).map(MembroDaStartup::getPapel)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Exigir quem edita
     * Objetivo: Recusar quem não pode editar uma seção do cadastro.
     * Decisões: Fundadores editam tudo; colaborador, só as seções autorizadas (KAN-65). Serviço público para as seções.
     */
    public void exigirEditor(UUID eu, UUID startupId, SecaoDaStartup secao) {
        MembroDaStartup membro = membros.findByStartupIdAndUsuarioIdAndSaiuEmIsNull(startupId, eu)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
        if (!membro.podeEditar(secao)) {
            throw new AccessDeniedException("Colaborador só edita as seções autorizadas pelo administrador.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Exigir fundador administrador
     * Objetivo: Recusar quem não é fundador administrador (gerenciar equipe, publicar, rodada, Nível 2).
     * Decisões: Membro de outro papel: 403; quem não é membro: 404. Serviço público.
     */
    public void exigirAdministrador(UUID eu, UUID startupId) {
        if (papel(eu, startupId) != PapelNaStartup.FUNDADOR_ADMINISTRADOR) {
            throw new AccessDeniedException("Só o fundador administrador pode fazer isso.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Conferir o cadastro
     * Objetivo: Validar e normalizar os dados cadastrais.
     * Decisões: Mensagens de negócio; a soma das participações não passa de 100%.
     */
    private Cadastro conferir(Cadastro c, UUID startupId) {
        String nome = c.nome() == null ? "" : c.nome().strip();
        if (nome.isEmpty() || nome.length() > 120) {
            throw new RegraDeNegocioException("nome_obrigatorio", "Informe o nome da startup (até 120 caracteres).");
        }
        if (c.segmento() == null || c.localizacao() == null) {
            throw new RegraDeNegocioException("campos_obrigatorios", "Escolha o segmento e a localização.");
        }
        String segmento = listas.exigirAtivo(Lista.SEGMENTO, c.segmento()).getCodigo();
        String localizacao = listas.exigirAtivo(Lista.LOCALIZACAO, c.localizacao()).getCodigo();
        String cnpj = null;
        if (c.cnpj() != null && !c.cnpj().isBlank()) {
            cnpj = Cnpj.validar(c.cnpj()).orElseThrow(() -> new RegraDeNegocioException("cnpj_invalido", "CNPJ inválido. Confira os 14 dígitos."));
            if (startups.existsByCnpjAndIdNot(cnpj, startupId == null ? UUID.randomUUID() : startupId)) {
                throw new RegraDeNegocioException("cnpj_em_uso", "Já existe uma startup com este CNPJ. Peça a um administrador dela para convidar você.");
            }
        }
        limite(c.nomeFantasia(), 120, "nome fantasia");
        limite(c.razaoSocial(), 200, "razão social");
        if (c.site() != null && !c.site().isBlank() && (!URL.matcher(c.site().strip()).matches() || c.site().length() > 200)) {
            throw new RegraDeNegocioException("site_invalido", "Informe o site começando por https://.");
        }
        Map<RedeSocial, String> redes = new EnumMap<>(RedeSocial.class);
        if (c.redes() != null) {
            c.redes().forEach((rede, url) -> {
                if (url != null && !url.isBlank()) {
                    if (!URL.matcher(url.strip()).matches() || url.length() > 300) {
                        throw new RegraDeNegocioException("rede_invalida", "Endereço inválido na rede " + rede + ".");
                    }
                    redes.put(rede, url.strip());
                }
            });
        }
        List<Fundador> fundadores = new ArrayList<>();
        BigDecimal soma = BigDecimal.ZERO;
        for (Fundador f : c.fundadores() == null ? List.<Fundador>of() : c.fundadores()) {
            String nomeFundador = f.nome() == null ? "" : f.nome().strip();
            if (nomeFundador.isEmpty() || nomeFundador.length() > 120) {
                throw new RegraDeNegocioException("fundador_nome", "Informe o nome de cada fundador.");
            }
            limite(f.cargo(), 80, "cargo");
            if (f.participacao() != null) {
                if (f.participacao().signum() < 0 || f.participacao().compareTo(BigDecimal.valueOf(100)) > 0 || f.participacao().scale() > 2) {
                    throw new RegraDeNegocioException("participacao_invalida", "A participação de cada fundador vai de 0% a 100%.");
                }
                soma = soma.add(f.participacao());
            }
            fundadores.add(new Fundador(nomeFundador, f.cargo() == null || f.cargo().isBlank() ? null : f.cargo().strip(), f.participacao()));
        }
        if (soma.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RegraDeNegocioException("participacao_acima", "A soma das participações dos fundadores passa de 100%.");
        }
        return new Cadastro(nome, c.nomeFantasia(), c.razaoSocial(), cnpj, segmento, localizacao, c.site(), redes, fundadores);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Limite de texto
     * Objetivo: Recusar texto opcional acima do tamanho da coluna.
     * Decisões: Mensagem com o nome do campo.
     */
    private static void limite(String texto, int maximo, String campo) {
        if (texto != null && texto.strip().length() > maximo) {
            throw new RegraDeNegocioException("texto_longo", "O campo " + campo + " pode ter até " + maximo + " caracteres.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Formato da imagem
     * Objetivo: Reconhecer PNG, JPEG e WebP pelos primeiros bytes.
     * Decisões: Não confia na extensão.
     */
    static Optional<String> formatoDaImagem(byte[] b) {
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of("image/png");
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E'
                && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Startup
     * Objetivo: Ler a startup pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Startup startup(UUID id) {
        return startups.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Vínculo
     * Objetivo: Levar uma startup com o papel da pessoa nela.
     * Decisões: Para a lista e o detalhe.
     */
    public record Vinculo(Startup startup, PapelNaStartup papel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Logotipo
     * Objetivo: Levar o conteúdo e o tipo do logotipo.
     * Decisões: Quem chama fecha o fluxo.
     */
    public record Logotipo(InputStream conteudo, String tipo) {
    }
}
