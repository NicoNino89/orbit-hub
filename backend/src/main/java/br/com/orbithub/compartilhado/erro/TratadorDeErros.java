package br.com.orbithub.compartilhado.erro;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Tratador global de erros da API
 * Objetivo: Responder todos os erros no formato Problem Details (RFC 9457), com mensagens em português.
 * Decisões: Herda o tratamento padrão do Spring MVC (400, 405, 415...) e acrescenta as exceções da
 *           plataforma. Erros de validação trazem o campo e a mensagem, para o formulário marcar o campo.
 *           Exceções inesperadas não são tratadas aqui: ficam com o 500 padrão, sem detalhes internos.
 */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Recurso não encontrado
     * Objetivo: Responder 404.
     * Decisões: Mensagem da exceção, pensada para o usuário.
     */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
        problema.setTitle("Não encontrado");
        return problema;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Regra de negócio violada
     * Objetivo: Responder 422 com o código da regra.
     * Decisões: O código vai na propriedade "codigo" do Problem Details.
     */
    @ExceptionHandler(RegraDeNegocioException.class)
    ProblemDetail regraDeNegocio(RegraDeNegocioException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, erro.getMessage());
        problema.setTitle("Não foi possível concluir");
        problema.setProperty("codigo", erro.getCodigo());
        return problema;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Dados inválidos no corpo
     * Objetivo: Responder 400 com a lista de campos inválidos.
     * Decisões: Um campo com mais de uma violação mostra a primeira; os valores enviados nunca voltam na
     *           resposta (podem ser senha).
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException erro,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        erro.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Confira os campos destacados.");
        problema.setTitle("Dados inválidos");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }
}
