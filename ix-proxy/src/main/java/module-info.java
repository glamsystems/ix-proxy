module systems.glam.ix_proxy {
  requires java.net.http;

  requires transitive systems.comodal.json_iterator;

  requires transitive software.sava.core;

  exports systems.glam.ix.proxy;
}
