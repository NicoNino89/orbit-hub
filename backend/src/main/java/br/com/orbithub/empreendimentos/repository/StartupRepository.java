package br.com.orbithub.empreendimentos.repository;

import br.com.orbithub.empreendimentos.domain.Startup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Repositório das startups
 * Objetivo: Ler e gravar startups.
 * Decisões: CNPJ único conferido antes de gravar, para a mensagem ser de negócio.
 */
public interface StartupRepository extends JpaRepository<Startup, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: CNPJ em uso por outra startup
     * Objetivo: Dizer se outra startup já usa o CNPJ.
     * Decisões: Ignora a própria startup, na edição.
     */
    boolean existsByCnpjAndIdNot(String cnpj, UUID id);
}
