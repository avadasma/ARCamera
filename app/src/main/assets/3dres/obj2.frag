precision mediump float;
varying vec2 textureCoordinate;
uniform sampler2D vTexture;
varying vec4 vDiffuse;          //Receive scattered light components coming from the vertex shader
varying vec4 vAmbient;          //Receive the ambient light component passed to the fragment shader
varying vec4 vSpecular;         //Receives the specular component passed to the fragment shader
void main() {
    vec4 finalColor=texture2D(vTexture,textureCoordinate);
//       //Give this element a meta color value
//    gl_FragColor=finalColor;
//vec4 finalColor=vec4(1.0);
       //Give this element a meta color value
    gl_FragColor=finalColor*vAmbient+finalColor*vSpecular+finalColor*vDiffuse;
}