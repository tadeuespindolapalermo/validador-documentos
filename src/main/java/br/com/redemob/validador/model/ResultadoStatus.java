package br.com.redemob.validador.model;

public enum ResultadoStatus {

	APROVADO("Dados confirmados", "Nenhuma divergencia encontrada."),
	DIVERGENTE("Divergencias encontradas", "Revise os campos destacados antes de aprovar o cadastro."),
	INCONCLUSIVO("Validacao inconclusiva", "A IA nao conseguiu confirmar todos os campos com seguranca."),
	ERRO("Validacao indisponivel", "Nao foi possivel concluir a analise agora.");

	private final String titulo;

	private final String descricao;

	ResultadoStatus(String titulo, String descricao) {
		this.titulo = titulo;
		this.descricao = descricao;
	}

	public String titulo() {
		return this.titulo;
	}

	public String descricao() {
		return this.descricao;
	}

}
