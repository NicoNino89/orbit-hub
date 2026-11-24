package br.com.orbithub.investidores.service;

import br.com.orbithub.investidores.domain.MembroDaOrganizacao;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.repository.MembroDaOrganizacaoRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Em nome de quem a pessoa atua
 * Objetivo: Resolver, a cada requisição, se o investidor age como pessoa ou em nome de uma organização, e conferir que
 *           ainda pode.
 * Decisões: A tela manda a organização escolhida no cabeçalho X-Orbit-Organizacao; sem ele, a ação é pessoal. O vínculo
 *           é lido do banco toda vez, então quem sai ou é removido perde o acesso na hora. Agir (interesse, Match,
 *           Nível 2) exige administrador ou membro; ver aceita também o observador. Toda ação guarda a pessoa
 *           ({@link Atuacao#pessoa()}), mesmo em nome da organização. Serviço público para os outros módulos.
 */
@Service
public class AtuacaoService {

    public static final String CABECALHO = "X-Orbit-Organizacao";

    private final MembroDaOrganizacaoRepository membros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criação do serviço
     * Objetivo: Receber o repositório dos membros.
     * Decisões: Injeção por construtor.
     */
    public AtuacaoService(MembroDaOrganizacaoRepository membros) {
        this.membros = membros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Atuação para agir
     * Objetivo: Resolver a atuação de quem vai agir (interesse, Match, Nível 2).
     * Decisões: Observador não age em nome da organização: 403.
     */
    @Transactional(readOnly = true)
    public Atuacao paraAgir(UUID pessoa, String cabecalho) {
        Atuacao atuacao = resolver(pessoa, cabecalho);
        if (atuacao.papel().map(p -> !p.podeAtuar()).orElse(false)) {
            throw new AccessDeniedException("Observador não pode agir em nome da organização.");
        }
        return atuacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Atuação para ver
     * Objetivo: Resolver a atuação de quem só vai consultar.
     * Decisões: Qualquer membro ativo.
     */
    @Transactional(readOnly = true)
    public Atuacao paraVer(UUID pessoa, String cabecalho) {
        return resolver(pessoa, cabecalho);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Resolver
     * Objetivo: Ler o cabeçalho e conferir o vínculo ativo.
     * Decisões: Cabeçalho inválido ou sem vínculo ativo: 403 (a pessoa saiu, foi removida ou nunca foi membro).
     */
    private Atuacao resolver(UUID pessoa, String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            return new Atuacao(pessoa, null, null);
        }
        UUID organizacao;
        try {
            organizacao = UUID.fromString(cabecalho.strip());
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Organização inválida.");
        }
        MembroDaOrganizacao vinculo = membros.findByOrganizacaoIdAndUsuarioIdAndSaiuEmIsNull(organizacao, pessoa)
                .orElseThrow(() -> new AccessDeniedException("Você não faz mais parte desta organização."));
        return new Atuacao(pessoa, organizacao, vinculo.getPapel());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Atuação
     * Objetivo: Levar quem agiu e, se for o caso, em nome de qual organização e com qual papel.
     * Decisões: Organização e papel nulos = ação pessoal.
     */
    public record Atuacao(UUID pessoa, UUID organizacao, PapelNaOrganizacao papelNaOrganizacao) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:05
         * Cartão Jira: KAN-60
         * Título: Em nome da organização
         * Objetivo: Dizer se a ação é da organização.
         * Decisões: Organização preenchida.
         */
        public boolean emNomeDaOrganizacao() {
            return organizacao != null;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 14:05
         * Cartão Jira: KAN-60
         * Título: Papel na organização
         * Objetivo: Devolver o papel, quando a ação é da organização.
         * Decisões: Vazio para ação pessoal.
         */
        public Optional<PapelNaOrganizacao> papel() {
            return Optional.ofNullable(papelNaOrganizacao);
        }
    }
}
