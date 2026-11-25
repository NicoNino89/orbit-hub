package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.domain.VisibilidadeDaStartup;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28, KAN-85
 * Título: Visibilidade e participação da startup
 * Objetivo: Ler e trocar o que a startup mostra no catálogo (Nível 1) e a escolha de participar das edições temáticas.
 * Decisões: Qualquer membro lê; só o fundador administrador troca, porque decide o que é divulgado (mesma regra de
 *           publicar). Só os itens diferentes do padrão são guardados. A troca vale na hora, inclusive com a startup
 *           publicada, e não passa pela curadoria.
 */
@Service
public class VisibilidadeService {

    private final StartupRepository startups;
    private final StartupsService cadastro;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Criação do serviço
     * Objetivo: Receber o repositório, o cadastro e o relógio.
     * Decisões: Injeção por construtor.
     */
    public VisibilidadeService(StartupRepository startups, StartupsService cadastro, Clock relogio) {
        this.startups = startups;
        this.cadastro = cadastro;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Ler visibilidade
     * Objetivo: Listar os itens com o padrão e a escolha atual, e a participação nas edições.
     * Decisões: Qualquer membro da equipe.
     */
    @Transactional(readOnly = true)
    public Configuracao ler(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        return configuracao(startup(startupId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Salvar visibilidade
     * Objetivo: Guardar quais itens são públicos.
     * Decisões: Recebe todos ou só alguns itens; os não enviados mantêm a escolha anterior. Código desconhecido é recusado.
     */
    @Transactional
    public Configuracao salvar(UUID eu, UUID startupId, Map<String, Boolean> escolhas) {
        cadastro.exigirAdministrador(eu, startupId);
        Startup s = startup(startupId);
        Map<String, Boolean> novas = new HashMap<>(s.visibilidade());
        (escolhas == null ? Map.<String, Boolean>of() : escolhas).forEach((codigo, publico) -> {
            VisibilidadeDaStartup.Item item = VisibilidadeDaStartup.item(codigo)
                    .orElseThrow(() -> new RegraDeNegocioException("item_invalido", "Item de visibilidade desconhecido: " + codigo + "."));
            if (publico == null || publico == item.padraoPublico()) {
                novas.remove(codigo);
            } else {
                novas.put(codigo, publico);
            }
        });
        s.definirVisibilidade(novas, relogio.instant());
        return configuracao(s);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Participar das edições
     * Objetivo: Registrar se a startup aceita aparecer nas edições temáticas.
     * Decisões: Só o fundador administrador.
     */
    @Transactional
    public Configuracao participarDeEdicoes(UUID eu, UUID startupId, boolean participa) {
        cadastro.exigirAdministrador(eu, startupId);
        Startup s = startup(startupId);
        s.definirParticipacaoEmEdicoes(participa, relogio.instant());
        return configuracao(s);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Montar configuração
     * Objetivo: Juntar o catálogo de itens às escolhas da startup.
     * Decisões: Na ordem das seções.
     */
    private static Configuracao configuracao(Startup s) {
        Map<String, Boolean> escolhas = s.visibilidade();
        List<ItemConfigurado> itens = VisibilidadeDaStartup.itens().stream()
                .map(i -> new ItemConfigurado(i, VisibilidadeDaStartup.publico(i.codigo(), escolhas))).toList();
        return new Configuracao(itens, s.isParticipaEdicoes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Startup
     * Objetivo: Ler a startup pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Startup startup(UUID id) {
        return startups.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Item configurado
     * Objetivo: Levar o item e se ele está público.
     * Decisões: O padrão vem no item.
     */
    public record ItemConfigurado(VisibilidadeDaStartup.Item item, boolean publico) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Configuração
     * Objetivo: Levar os itens e a participação nas edições.
     * Decisões: Uma leitura para a aba inteira.
     */
    public record Configuracao(List<ItemConfigurado> itens, boolean participaEdicoes) {
    }
}
