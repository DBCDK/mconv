{
  description = "mconv flake";
 
  # Here be dragons
  nixConfig.sandbox = "relaxed";

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
            (final: prev: {
              # force maven to use graalvm JDK
              maven = prev.maven.override { jdk = prev.graalvm-ce; };
            })
          ];
        };
        jdk = pkgs.graalvm-ce;

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

          # Here be dragons.
          # Turn off the sandbox and give the build internet access,
          # thus effectively giving up build reproducebility.
          __noChroot = true;

          src = ./.;

          buildInputs = commonInputs ++ jvmInputs;

          buildPhase = "mvn -Dmaven.repo.local=.m2 -V -DskipTests=true -P native verify";

          installPhase = ''
             mkdir -p $out/bin

             cp cli/target/mconv $out/bin
          '';
        };
      }
    );
}
