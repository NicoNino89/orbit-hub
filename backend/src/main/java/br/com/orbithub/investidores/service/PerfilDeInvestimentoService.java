package br.com.orbithub.investidores.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.domain.Organizacao;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento.Dados;
import br.com.orbithub.investidores.repository.OrganizacaoRepository;
import br.com.orbithub.investidores.repository.PerfilDeInvestimentoRepository;
import br.com.orbithub.investidores.service.AtuacaoService.Atuacao;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Perfil de investimento
 * Objetivo: Ler e salvar o tipo de investidor, o ticket, a liderança, o follow-on, a participação na gestão e os
 *           instrumentos aceitos, da pessoa ou da organização em nome da qual ela atua.
 * Decisões: O dono vem da atuação (KAN-60). Na organização, o tipo é o da própria organização e só o administrador
 *           altera; membros e observadores veem. Na pessoa física, o tipo é investidor-anjo ou "outros" (com descrição).
 *           Instrumentos vêm da lista gerenciável e precisam estar ativos.
 */
@Service
public class PerfilDeInvestimentoService {

    static final Set<String> TIPOS_DE_PESSOA = Set.of("ANJO", "OUTROS");
    static final String OUTROS = "OUTROS";
    static final BigDecimal TICKET_LIMITE = new BigDecimal("9999999999999.99");

    private final PerfilDeInvestimentoRepository perfis;
    private final OrganizacaoRepository organizacoes;
    private final ListasService listas;
    private final ConsultaDeContas contas;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, listas, contas e relógio.
     * Decisões: Injeção por construtor.
     */
    public PerfilDeInvestimentoService(PerfilDeInvestimentoRepository perfis, OrganizacaoRepository organizacoes,
                                       ListasService listas, ConsultaDeContas contas, Clock relogio) {
        this.perfis = perfis;
        this.organizacoes = organizacoes;
        this.listas = listas;
        this.contas = contas;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Ler o perfil
     * Objetivo: Devolver o perfil do dono da atuação, se cadastrado, e o que a pessoa pode fazer com ele.
     * Decisões: Na organização, informa o tipo fixo e se quem vê pode editar.
     */
    @Transactional(readOnly = true)
    public Situacao ler(Atuacao atuacao) {
        Optional<PerfilDeInvestimento> perfil = buscar(atuacao);
        String tipoFixo = atuacao.emNomeDaOrganizacao() ? organizacao(atuacao.organizacao()).getTipo() : null;
        return new Situacao(atuacao.emNomeDaOrganizacao(), podeEditar(atuacao), tipoFixo, perfil.map(PerfilDeInvestimento::dados),
                perfil.flatMap(p -> contas.nome(p.getAtualizadoPor())), perfil.map(PerfilDeInvestimento::getAtualizadoEm));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Tem perfil
     * Objetivo: Dizer se o dono da atuação já cadastrou o perfil.
     * Decisões: Para a área do investidor.
     */
    @Transactional(readOnly = true)
    public boolean cadastrado(Atuacao atuacao) {
        return buscar(atuacao).isPresent();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Salvar o perfil
     * Objetivo: Criar ou atualizar o perfil do dono da atuação, com as validações.
     * Decisões: Na organização só o administrador (403); o tipo é o da organização. Registra quem salvou.
     */
    @Transactional
    public Situacao salvar(Atuacao atuacao, Dados pedido) {
        if (!podeEditar(atuacao)) {
            throw new AccessDeniedException("Só o administrador da organização altera o perfil de investimento dela.");
        }
        Dados dados = conferir(atuacao, pedido);
        Instant agora = relogio.instant();
        PerfilDeInvestimento perfil = buscar(atuacao)
                .orElseGet(() -> PerfilDeInvestimento.novo(atuacao.pessoa(), atuacao.organizacao(), agora));
        perfil.atualizar(dados, atuacao.pessoa(), agora);
        perfis.save(perfil);
        return ler(atuacao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Conferir os dados
     * Objetivo: Validar tipo, ticket, liderança, participação e instrumentos, e normalizar os textos.
     * Decisões: Mensagens de negócio, para os formulários.
     */
    private Dados conferir(Atuacao atuacao, Dados pedido) {
        String tipo;
        String tipoOutro = null;
        if (atuacao.emNomeDaOrganizacao()) {
            tipo = organizacao(atuacao.organizacao()).getTipo();
        } else {
            tipo = pedido.tipo();
            if (!TIPOS_DE_PESSOA.contains(tipo)) {
                throw new RegraDeNegocioException("tipo_de_pessoa",
                        "Como pessoa física, escolha investidor-anjo ou outro. Fundos e redes atuam como organização.");
            }
            listas.exigirAtivo(Lista.TIPO_INVESTIDOR, tipo);
            if (OUTROS.equals(tipo)) {
                tipoOutro = pedido.tipoOutro() == null ? "" : pedido.tipoOutro().strip();
                if (tipoOutro.isEmpty() || tipoOutro.length() > 120) {
                    throw new RegraDeNegocioException("tipo_outro", "Descreva o seu tipo de investidor (até 120 caracteres).");
                }
            }
        }
        BigDecimal minimo = pedido.ticketMinimo();
        BigDecimal maximo = pedido.ticketMaximo();
        if (minimo == null || maximo == null || minimo.signum() <= 0 || maximo.compareTo(minimo) < 0
                || maximo.compareTo(TICKET_LIMITE) > 0 || minimo.scale() > 2 || maximo.scale() > 2) {
            throw new RegraDeNegocioException("ticket_invalido",
                    "Informe o ticket mínimo (maior que zero) e o máximo (igual ou maior que o mínimo), em reais.");
        }
        if (pedido.lideranca() == null || pedido.participacaoNaGestao() == null) {
            throw new RegraDeNegocioException("campos_obrigatorios", "Informe a liderança e a participação na gestão.");
        }
        if (pedido.instrumentos() == null || pedido.instrumentos().isEmpty()) {
            throw new RegraDeNegocioException("sem_instrumento", "Escolha pelo menos um instrumento de investimento.");
        }
        Set<String> instrumentos = new TreeSet<>();
        pedido.instrumentos().forEach(i -> instrumentos.add(listas.exigirAtivo(Lista.INSTRUMENTO, i).getCodigo()));
        return new Dados(tipo, tipoOutro, minimo, maximo, pedido.lideranca(), pedido.followOn(),
                pedido.participacaoNaGestao(), instrumentos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Pode editar
     * Objetivo: Dizer se quem atua pode alterar o perfil.
     * Decisões: Pessoa física sempre; organização só o administrador.
     */
    private static boolean podeEditar(Atuacao atuacao) {
        return !atuacao.emNomeDaOrganizacao() || atuacao.papel().orElse(null) == PapelNaOrganizacao.ADMINISTRADOR;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Buscar o perfil
     * Objetivo: Ler o perfil do dono da atuação.
     * Decisões: Pela organização ou pela pessoa.
     */
    private Optional<PerfilDeInvestimento> buscar(Atuacao atuacao) {
        return atuacao.emNomeDaOrganizacao() ? perfis.findByOrganizacaoId(atuacao.organizacao())
                : perfis.findByPessoaId(atuacao.pessoa());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Organização
     * Objetivo: Ler a organização da atuação.
     * Decisões: 404 se não existir (a atuação já conferiu o vínculo).
     */
    private Organizacao organizacao(UUID id) {
        return organizacoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Organização não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Situação do perfil
     * Objetivo: Levar o perfil (se houver), o dono, se pode editar, o tipo fixo da organização e a última alteração.
     * Decisões: Campos opcionais quando ainda não há perfil.
     */
    public record Situacao(boolean daOrganizacao, boolean podeEditar, String tipoFixo, Optional<Dados> perfil,
                           Optional<String> atualizadoPor, Optional<Instant> atualizadoEm) {
    }
}
