package br.com.redemob.validador.model;

public enum StatusCampo {

	CONFIRMADO("Confirmado"),
	DIVERGENTE("Divergente"),
	NAO_LOCALIZADO("Nao localizado");

	private final String rotulo;

	StatusCampo(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return this.rotulo;
	}

}
