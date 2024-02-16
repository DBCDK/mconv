{
  description = "mconv flake";

  inputs = {
    nixpkgs.url = github:nixos/nixpkgs/nixos-23.05; # Use deprecated channel to get graalvm based on JDK 11
    flake-utils.url = github:numtide/flake-utils;
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [
            (f: p: {
              # force maven to use graalvm JDK
              maven = p.maven.override { jdk = p.graalvm-ce; };
            })
          ];
        };
        jdk = pkgs.graalvm-ce;

        maven-repository = (pkgs.buildMaven ./project-info.json).repo;

        # double invocation
        maven-repository-mconv = pkgs.stdenv.mkDerivation {
          name = "maven-repository";
          buildInputs = [ pkgs.maven ];
          src = ./.; # or fetchFromGitHub, cleanSourceWith, etc
          buildPhase = ''
          mvn -Dmaven.repo.local=$out -DskipTests=false package
          '';

          # keep only *.{pom,jar,sha1,nbm} and delete all ephemeral files
          # with lastModified timestamps inside
          installPhase = ''
          find $out -type f \
          -name \*.lastUpdated -or \
          -name resolver-status.properties -or \
          -name _remote.repositories \
          -delete
          '';

          # don't do any fixup
          dontFixup = true;
          outputHashAlgo = "sha256";
          outputHashMode = "recursive";
          # replace this with the correct SHA256
          #outputHash = pkgs.lib.fakeSha256;
          outputHash = "sha256-hWb5pDDaNeroOMm35FnULaQdV2yHm8cGGD66nPcz52g=";
        };

        commonInputs = with pkgs; [
        ];

        jvmInputs = with pkgs; [
          jdk
          maven

        ];
        jvmHook = ''
          JAVA_HOME="${jdk}"
        '';
      in
      {
        devShells.default = pkgs.mkShell {
          name = "mconv-dev-shell";
          buildInputs = commonInputs ++ jvmInputs;
          shellHook = jvmHook;
        };

        packages.default = pkgs.stdenv.mkDerivation {
          pname = "mconv";
          version = "2.0";

          src = ./.;

          buildInputs = commonInputs ++ jvmInputs;

          buildPhase = "mvn --offline -Dmaven.repo.local=${maven-repository-mconv} -V clean";
          #buildPhase = "mvn -Dmaven.repo.local=.m2 -V -P native verify";

          #installPhase = ''
         #   mkdir -p $out/bin
          #  mkdir -p $out/share/java
#
#            cp target/scala-2.13/*.jar $out/share/java
#
#            makeWrapper ${pkgs.jdk17_headless}/bin/java $out/bin/nix-scala-example \
#              --add-flags "-cp \"$out/share/java/*\" com.example.nixscalaexample.Main"
#          '';
        };
      }
    );
}
