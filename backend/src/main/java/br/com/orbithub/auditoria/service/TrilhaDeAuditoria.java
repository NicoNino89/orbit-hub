package br.com.orbithub.auditoria.service;

import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:05
 * Cartão Jira: KAN-80
 * Título: Trilha de auditoria
 * Objetivo: Gravar as ações da equipe interna e permitir a consulta com filtros por pessoa, período e ação.
 * Decisões: O nome do autor é copiado no momento da gravação. A gravação participa da transação de quem
 *           chama (se a ação for desfeita, o registro de sucesso também é); fora de transação, abre a sua.
 *           A consulta pagina no banco e limita o tamanho da página, para a tela não puxar a trilha inteira.
 */
@Service
public class TrilhaDeAuditoria {

    static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final RegistroAuditoriaRepository registros;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:05
     * Cartão Jira: KAN-80
     * Título: Criação do serviço
     * Objetivo: Receber o repositório, as contas (para o nome do autor) e o relógio.
     * Decisões: Relógio injetado, para os testes controlarem o tempo.
     */
    public TrilhaDeAuditoria(RegistroAuditoriaRepository registros, UsuarioRepository usuarios, Clock relogio) {
        this.registros = registros;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:05
     * Cartão Jira: KAN-80
     * Título: Registrar uma ação
     * Objetivo: Gravar uma ação na trilha.
     * Decisões: Se o nome do autor não vier, busca pela conta; conta inexistente grava só o id.
     */
    @Transactional
    public RegistroAuditoria registrar(Acao acao) {
        Acao completa = acao;
        if (acao.atorId() != null && acao.atorNome() == null) {
            String nome = usuarios.findById(acao.atorId()).map(Usuario::getNome).orElse(null);
            completa = new Acao(acao.atorId(), nome, acao.acao(), acao.objetoTipo(), acao.objetoId(),
                    acao.resultado(), acao.statusHttp(), acao.detalhe(), acao.justificativa());
        }
        return registros.save(RegistroAuditoria.de(completa, relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:05
     * Cartão Jira: KAN-80
     * Título: Consultar a trilha
     * Objetivo: Buscar registros por pessoa, período, ação e objeto, do mais recente para o mais antigo.
     * Decisões: Todos os filtros são opcionais; período invertido é recusado com mensagem clara; página
     *           maior que o limite é reduzida ao limite.
     */
    @Transactional(readOnly = true)
    public Page<RegistroAuditoria> consultar(Filtro filtro, int pagina, int tamanho) {
        if (filtro.de() != null && filtro.ate() != null && filtro.de().isAfter(filtro.ate())) {
            throw new RegraDeNegocioException("periodo_invalido", "A data inicial precisa ser anterior à final.");
        }
        int tamanhoValido = Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA);
        PageRequest pedido = PageRequest.of(Math.max(pagina, 0), tamanhoValido,
                Sort.by(Sort.Direction.DESC, "ocorridoEm"));
        return registros.findAll(especificacao(filtro), pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:05
     * Cartão Jira: KAN-80
     * Título: Ações conhecidas
     * Objetivo: Listar os códigos de ação presentes na trilha, para o filtro por tipo de ação.
     * Decisões: Lidos do banco, para a lista acompanhar as ações novas sem manutenção.
     */
    @Transactional(readOnly = true)
    public List<String> acoes() {
        return registros.acoesRegistradas();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:45
     * Cartão Jira: KAN-80
     * Título: Pessoas presentes na trilha
     * Objetivo: Listar quem já fez ações, para o filtro por pessoa.
     * Decisões: Lidas da própria trilha, assim aparecem também pessoas já desligadas.
     */
    @Transactional(readOnly = true)
    public List<RegistroAuditoriaRepository.Ator> atores() {
        return registros.atoresRegistrados();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:05
     * Cartão Jira: KAN-80
     * Título: Montagem do filtro
     * Objetivo: Converter o filtro em condições da consulta.
     * Decisões: Período fechado no início e aberto no fim (de <= ocorrido < até).
     */
    static Specification<RegistroAuditoria> especificacao(Filtro filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.atorId() != null) {
                condicoes.add(cb.equal(raiz.get("atorId"), filtro.atorId()));
            }
            if (filtro.acao() != null && !filtro.acao().isBlank()) {
                condicoes.add(cb.equal(raiz.get("acao"), filtro.acao().strip()));
            }
            if (filtro.objetoTipo() != null && !filtro.objetoTipo().isBlank()) {
                condicoes.add(cb.equal(raiz.get("objetoTipo"), filtro.objetoTipo().strip()));
            }
            if (filtro.objetoId() != null && !filtro.objetoId().isBlank()) {
                condicoes.add(cb.equal(raiz.get("objetoId"), filtro.objetoId().strip()));
            }
            if (filtro.de() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(raiz.get("ocorridoEm"), filtro.de()));
            }
            if (filtro.ate() != null) {
                condicoes.add(cb.lessThan(raiz.get("ocorridoEm"), filtro.ate()));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:05
     * Cartão Jira: KAN-80
     * Título: Filtro da consulta
     * Objetivo: Agrupar os filtros aceitos pela trilha.
     * Decisões: Campos nulos não filtram.
     */
    public record Filtro(UUID atorId, String acao, String objetoTipo, String objetoId, Instant de, Instant ate) {
    }
}
