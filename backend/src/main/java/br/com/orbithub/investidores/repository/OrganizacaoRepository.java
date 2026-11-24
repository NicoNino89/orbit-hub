package br.com.orbithub.investidores.repository;

import br.com.orbithub.investidores.domain.Organizacao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Repositório das organizações
 * Objetivo: Ler e gravar organizações investidoras.
 * Decisões: CNPJ único também conferido antes de gravar, para a mensagem ser de negócio.
 */
public interface OrganizacaoRepository extends JpaRepository<Organizacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: CNPJ em uso
     * Objetivo: Dizer se já existe organização com o CNPJ.
     * Decisões: CNPJ normalizado.
     */
    boolean existsByCnpj(String cnpj);
}
