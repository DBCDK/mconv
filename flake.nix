{
  description = "mconv 2.0";

  inputs = {
    nixpkgs.url = github:nixos/nixpkgs/nixos-23.11; # Use deprecated channel to get graalvm based on JDK 11
    flake-parts.url = "github:hercules-ci/flake-parts";
    flake-compat.url = "github:edolstra/flake-compat";
    flake-compat.flake = false;
    mavenix.url = "github:nix-community/mavenix";
    mavenix.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = inputs@{ self, flake-parts, nixpkgs, mavenix, ... }:
    let
    pname="mconv";
    in
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = nixpkgs.lib.systems.flakeExposed ;

      flake = {
          overlays.default = final: prev: {
            "${pname}" = final.callPackage ./default.nix {
              src = final.nix-gitignore.gitignoreSource [] ./.;
              mavenix = import mavenix { pkgs = final; };
            };
          };
      };

      perSystem = { config, pkgs, system, ... }:
      let
        pname="mconv";

        overlays_default = final: prev: {
            # force maven to use graalvm JDK
            maven = prev.maven.override { jdk = prev.graalvm-ce; };
        };

        pkgs = import nixpkgs {
                  inherit system;
                  overlays = [
                      overlays_default mavenix.overlay
                  ];
                };
         buildPackages = [ pkgs.graalvm-ce  ];
      in rec {
        devShells.default = pkgs.mkShell {
                name = "dev-env ${pname}";
                buildInputs = [
                  pkgs.graalvm-ce
                  pkgs.maven
                  mavenix.defaultPackage.${system} ] ;
        };
        packages.default = packages.mconv;

        packages.mconv = pkgs.callPackage ./default.nix {
            src = pkgs.nix-gitignore.gitignoreSource [] ./.;
            mavenix = import mavenix { pkgs = pkgs; };
        };



      };
    };
}
