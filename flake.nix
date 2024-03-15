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

  outputs = inputs@{ flake-parts, nixpkgs, mavenix, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {

      systems = nixpkgs.lib.systems.flakeExposed ;
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
      in {
        devShells.default = pkgs.mkShell {
                name = "dev-env ${pname}";
                buildInputs = [ pkgs.graalvm-ce pkgs.maven  ] ;
        };


      };
    };
}
