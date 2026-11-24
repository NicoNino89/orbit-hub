package br.com.orbithub.identidade.repository;

import br.com.orbithub.identidade.domain.BloqueioDeLogin;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Repositório de bloqueios de login
 * Objetivo: Ler e gravar a contagem de falhas por hash de e-mail.
 * Decisões: A chave primária é o próprio hash.
 */
public interface BloqueioDeLoginRepository extends JpaRepository<BloqueioDeLogin, String> {
}
