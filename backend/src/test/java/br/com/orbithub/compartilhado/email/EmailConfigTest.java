package br.com.orbithub.compartilhado.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Testes da escolha do provedor de e-mail
 * Objetivo: Verificar os adaptadores de SMTP e de registro e a recusa de tipo inválido.
 * Decisões: JavaMailSender simulado.
 */
class EmailConfigTest {

    private final EnvioDeEmail.MensagemEmail mensagem =
            new EnvioDeEmail.MensagemEmail("ana@exemplo.com", "Assunto", "Texto", "teste");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Envio por SMTP
     * Objetivo: Garantir remetente, destinatário, assunto e texto da mensagem enviada.
     * Decisões: Confere o SimpleMailMessage capturado.
     */
    @Test
    @SuppressWarnings("unchecked")
    void smtpMontaAMensagem() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provedor = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provedor.getObject()).thenReturn(smtp);

        new EmailConfig().envioDeEmail("smtp", "Orbit <nao-responda@orbit>", provedor).enviar(mensagem);

        ArgumentCaptor<SimpleMailMessage> enviada = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(enviada.capture());
        assertThat(enviada.getValue().getFrom()).isEqualTo("Orbit <nao-responda@orbit>");
        assertThat(enviada.getValue().getTo()).containsExactly("ana@exemplo.com");
        assertThat(enviada.getValue().getSubject()).isEqualTo("Assunto");
        assertThat(enviada.getValue().getText()).isEqualTo("Texto");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Falha de SMTP não propaga
     * Objetivo: Garantir que a falha de envio não derruba quem chamou.
     * Decisões: SMTP lança MailSendException.
     */
    @Test
    @SuppressWarnings("unchecked")
    void falhaDeSmtpNaoPropaga() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        doThrow(new MailSendException("fora do ar")).when(smtp).send(any(SimpleMailMessage.class));
        ObjectProvider<JavaMailSender> provedor = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provedor.getObject()).thenReturn(smtp);

        assertThatCode(() -> new EmailConfig().envioDeEmail("smtp", "x", provedor).enviar(mensagem))
                .doesNotThrowAnyException();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Registro e tipo inválido
     * Objetivo: Garantir que o modo registro não envia nada e que tipo desconhecido falha na inicialização.
     * Decisões: O registro aceita a mensagem sem erro; a mensagem não expõe o endereço no toString.
     */
    @Test
    @SuppressWarnings("unchecked")
    void registroETipoInvalido() {
        ObjectProvider<JavaMailSender> provedor = mock(ObjectProvider.class);
        assertThatCode(() -> new EmailConfig().envioDeEmail("registro", "x", provedor).enviar(mensagem))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new EmailConfig().envioDeEmail("pombo", "x", provedor))
                .isInstanceOf(IllegalStateException.class);
        assertThat(mensagem.toString()).isEqualTo("MensagemEmail[teste]");
    }
}
