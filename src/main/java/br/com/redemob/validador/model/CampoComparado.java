package br.com.redemob.validador.model;

public record CampoComparado(String rotulo, String informado, String extraido, StatusCampo status, String detalhe) {

	public boolean divergente() {
		return this.status == StatusCampo.DIVERGENTE;
	}

	public boolean confirmado() {
		return this.status == StatusCampo.CONFIRMADO;
	}

}
