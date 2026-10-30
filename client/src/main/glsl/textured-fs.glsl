#version 300 es
precision highp float;

out vec4 fragmentColor;
in vec4 texCoord;
in vec4 worldNormal;
in vec4 worldTangent;
in vec4 worldPosition;

uniform struct{
  mat4 viewProjMatrix;
  vec3 position;
} camera;

uniform struct {
  vec4 position;
  vec3 powerDensity;
} lights[8];

uniform struct{
  sampler2D colorTexture;
  vec3 ks;
  float shininess;
} material;

vec3 shade(
  vec3 powerDensity,
  vec3 normal,
  vec3 lightDir,
  vec3 viewDir,
  vec3 kd) {

  float cosa =
    clamp( dot(lightDir, normal), 0.0, 1.0);
  vec3 halfway = normalize(viewDir + lightDir);
  return
    powerDensity * cosa * kd
    +
    powerDensity * pow(clamp(dot(halfway, normal), 0.0, 1.0), material.shininess) * material.ks
    ;
}

void main(void) {
  vec3 normal = normalize(worldNormal.xyz);

  vec3 x = worldPosition.xyz / worldPosition.w;
  vec3 viewDir = normalize(camera.position - x);

  vec3 kd = texture(material.colorTexture, texCoord.xy).rgb;

  vec3 radianceToEye = kd * 0.2; // ambient
  for(int i=0; i<1; i++){
    vec3 lightDiff = lights[i].position.xyz -
                 x * lights[i].position.w;
    float lightDistSquared = dot(lightDiff, lightDiff);
    vec3 lightDir = lightDiff / sqrt(lightDistSquared);
    vec3 powerDensity = lights[i].powerDensity / lightDistSquared;

    radianceToEye += shade(
      powerDensity,
      normal,
      lightDir,
      viewDir,
      kd
    );
  }

  fragmentColor = vec4(radianceToEye, 1);
}
